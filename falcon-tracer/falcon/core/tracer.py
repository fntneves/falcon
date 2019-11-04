import multiprocessing
import argparse
import logging
import logging.config
import events
import signal
import time
import sys
import errno
import os
from falcon import util
from falcon.core import settings
from falcon.core.events.handling.writers.writer_factory import WriterFactory
import pkg_resources
from falcon import util

# Configure logger
logging.config.fileConfig(pkg_resources.resource_filename('falcon', '../conf/logging.ini'))

class Tracer:
    def __init__(self, pid=None, comm=None, should_signal=False):
        self.pid = pid
        self.comm = comm
        self.on_ready_callback = None
        if should_signal and pid is not None:
            self.on_ready_callback = lambda : os.kill(pid, signal.SIGCONT)

    def run(self):
        from bpf import BpfProgram

        program_filepath = pkg_resources.resource_filename('falcon', 'core/resources/ebpf/probes.c')

        with open(program_filepath, 'r') as program_file:
            program = BpfProgram(text=program_file.read())
            program.filter_pid(self.pid or 0)
            program.filter_comm(self.comm)

            logging.info('Creating and booting event dispatcher and handlers...')

            handler = events.handling.FalconEventLogger(WriterFactory.createFromConfig())
            handler.boot()

            logging.info('Running eBPF listener...')
            bpf_listener_worker = events.bpf.BpfEventListener(program, handler, self.pid, self.on_ready_callback)
            bpf_listener_worker.run()

            logging.info('Shutting handler down...')
            handler.shutdown()

        return 0

def run_tracer(target_pid):
    pid = os.fork()
    if pid == 0:
        try:
            # Ask for sudo permissions for correctly running tracer
            program = ['sudo' ,'falcon-tracer', '--pid', str(target_pid), '--signal']
            logging.debug("Starting tracer with cmd [" + " ".join(program) + "].")

            os.execvp(program[0], program)
        except Exception as e:
            logging.error("Could not execute tracer: {}".format(e))
            os._exit(1)
    else:
        return pid

def run_program(program):
    pid = os.fork()
    if pid == 0:
        paused = [True]
        def received(signum, frame):
            paused[0] = False

        signal.signal(signal.SIGCONT, received)

        while paused[0]:
            signal.pause()

        logging.info("Started program ["  + " ".join(program) + "]")

        try:
            os.execvp(program[0], program)
        except Exception as e:
            logging.error("Could not execute program: {}".format(e))
            os._exit(1)
    else:
        return pid

def main():
    """Main entry point for the script."""
    pid_file = '/tmp/tracer.pid'

    if (util.check_pid(util.read_pid(pid_file))):
        raise RuntimeError("Tracer is already running. Please stop it before starting a new instance.")

    parser = argparse.ArgumentParser(prog='falcon-tracer', description='This program is the tracer of the Falcon pipeline tool.')

    parser.add_argument('--comm', default=None,
                        help="filter events of the given COMM.")
    parser.add_argument('--pid', type=int, nargs='?', default=None,
                        help="filter events of the given PID.")
    parser.add_argument('--signal', action='store_true',
                        help="send SIGCONT signal to the given PID.")
    args, cmd = parser.parse_known_args()

    if args.pid is None and args.comm is None:
        exit = False
        program_exited = False
        tracer_exited = False
        def ignore(signum, frame):
            pass

        # Ignore CTRL+C interruptions to wait for all children
        signal.signal(signal.SIGINT, ignore)

        # Execute command received as argument
        target_pid = run_program(cmd)
        logging.info("Launched program ["  + " ".join(cmd) + "]. PID is [" + str(target_pid) + "]")

        # Execute tracer with argument pid
        tracer_wrapper_pid = run_tracer(target_pid)

        # Wait for child processes to terminate
        while exit is False:
            try:
                if program_exited is False:
                    os.waitpid(target_pid, 0)
                    program_exited = True
                    logging.info("The target program [" + str(target_pid) + "] exited.")

                if tracer_exited is False:
                    tracer_pid = util.read_pid(pid_file)
                    logging.debug("Interrupting tracer [" + str(tracer_pid) + "] program.")
                    os.system("sudo kill -SIGINT " + str(tracer_pid))
                    os.waitpid(tracer_wrapper_pid, 0)
                    tracer_exited = True
                    logging.info("The tracer program [" + str(tracer_pid) + "] exited.")

                if program_exited and tracer_exited:
                    exit = True
            except OSError, e:
                if e.errno != errno.EINTR:
                    raise
    else:
        util.write_pid(pid_file)
        Tracer(args.pid, args.comm, args.signal).run()
        util.clean_pid(pid_file)

if __name__ == '__main__':
    main()

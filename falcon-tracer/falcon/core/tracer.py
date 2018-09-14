import multiprocessing
import argparse
import logging
import logging.config
import events
import signal
import time
import sys
import os
from bpf import BpfProgram
from falcon import util
from falcon.core import settings
from falcon.core.events.handling.writers.writer_factory import WriterFactory
import pkg_resources

# Configure logger
logging.config.fileConfig(pkg_resources.resource_filename('falcon', '../conf/logging.ini'))

class Tracer:
    def run(self, pid=None, comm=None, signal_child=False):
        program_filepath = pkg_resources.resource_filename('falcon', 'core/resources/ebpf/probes.c')

        def shutdown_tracer(signum, frame):
            os.kill(multiprocessing.current_process().pid, signal.SIGINT)
        signal.signal(signal.SIGCHLD, shutdown_tracer)

        with open(program_filepath, 'r') as program_file:
            program = BpfProgram(text=program_file.read())

            program.filter_pid(pid or 0)
            program.filter_comm(comm)

            logging.info('Creating and booting handlers and appenders...')
            handler = events.handling.SysGraph()
            handler.boot()

            logging.info('Running eBPF listener...')
            bpf_listener_worker = events.bpf.BpfEventListener(program, handler, pid)
            bpf_listener_worker.run(signal_child)

            logging.info('Shutting handler down...')
            handler.shutdown()

        return 0

def run_program(program):
    pid = os.fork()
    if pid == 0:
        paused = [True]
        def received(signum, frame):
            paused[0] = False

        signal.signal(signal.SIGCONT, received)

        while paused[0]:
            signal.pause()
        try:
            os.execvp(program[0], program)
        except Exception as e:
            logging.error("Could not execute program: {}".format(e))
            os._exit(1)
    else:
        return pid


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(prog='falcon-tracer', description='This program is the tracer of the Falcon pipeline tool.')
    process_filter = parser.add_mutually_exclusive_group()
    process_filter.add_argument('--pid', type=int, nargs='?',
                        help="filter events of the given PID.")
    process_filter.add_argument('--comm', type=str, nargs='?',
                        help="filter events of the given comm processes.")
    args, cmd = parser.parse_known_args()

    tracer_options = {
        'signal_child': False,
    }

    if args.pid is not None:
        tracer_options['pid'] = args.pid
    elif args.comm is not None:
        tracer_options['comm'] = args.comm
    else:
        prog_pid = run_program(cmd)
        tracer_options['signal_child'] = True
        tracer_options['pid'] = prog_pid

    sys.exit(Tracer().run(**tracer_options))

if __name__ == '__main__':
    main()

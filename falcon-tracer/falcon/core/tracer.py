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
import pkg_resources

# Configure logger
logging.config.fileConfig(pkg_resources.resource_filename('falcon', '../conf/logging.ini'))

class Tracer:
    def run(self, pid=0, signal_child=False):
        program_filepath = pkg_resources.resource_filename('falcon', 'core/resources/ebpf/probes.c')

        def shutdown_tracer(signum, frame):
            os.kill(multiprocessing.current_process().pid, signal.SIGINT)
        signal.signal(signal.SIGCHLD, shutdown_tracer)

        with open(program_filepath, 'r') as program_file:
            program = BpfProgram(text=program_file.read())
            program.filter_pid(pid)

            logging.info('Creating and booting handlers...')
            handlers = []
            for handler in Tracer.get_handlers():
                handler.boot()
                handlers.append(handler)

            logging.info('Running eBPF listener...')
            bpf_listener_worker = events.bpf.BpfEventListener(program, handlers[0], pid)
            bpf_listener_worker.run(signal_child)

            # Shutdown handlers
            logging.info('Shutting handlers down...')
            for handler in handlers:
                handler.shutdown()

        return 0

    @staticmethod
    def get_handlers():
        handler_instances = [
            events.handling.logger.EventLogger()
        ]

        return handler_instances

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
        except:
            logging.error("Could not execute program")
            os._exit(1)
    else:
        return pid


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(prog='falcon-tracer', description='This program is the tracer of the Falcon pipeline tool.')

    parser.add_argument('--pid', type=int, nargs='?', default=-1,
                        help="filter events of the given PID.")
    args, cmd = parser.parse_known_args()
    signal_child = False
    if args.pid == -1:
        prog_pid = run_program(cmd)
        signal_child = True
    else:
        prog_pid = args.pid
    sys.exit(Tracer().run(
        pid=prog_pid, signal_child=signal_child))

if __name__ == '__main__':
    main()

import multiprocessing
import argparse
import logging
import logging.config
import events
import signal
import time
import sys
from bpf import BpfProgram
from falcon import util
import pkg_resources

# Configure logger
logging.config.fileConfig(pkg_resources.resource_filename('falcon', '../conf/logging.ini'))

class Tracer:
    def run(self, pid=0):
        program_filepath = pkg_resources.resource_filename('falcon', 'core/resources/ebpf/probes.c')

        with open(program_filepath, 'r') as program_file:
            program = BpfProgram(text=program_file.read())
            program.filter_pid(pid)

            logging.info('Creating and booting handlers...')
            handlers = []
            for handler in Tracer.get_handlers():
                handler.boot()
                handlers.append(handler)

            logging.info('Running eBPF listener...')
            bpf_listener_worker = events.bpf.BpfEventListener(program, handlers[0])
            bpf_listener_worker.run()

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


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(prog='falcon-tracer', description='This program is the tracer of the Falcon pipeline tool.')

    parser.add_argument('--pid', type=int, nargs='?', default=0,
                        help="filter events of the given PID. (0 = all PIDs)")
    args = parser.parse_args()
    sys.exit(Tracer().run(
        pid=args.pid))

if __name__ == '__main__':
    main()

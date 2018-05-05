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
    def run(self, pid=0):
        program_filepath = pkg_resources.resource_filename('falcon', 'core/resources/ebpf/probes.c')

        def ignore_signal(signum, frame):
            pass
        signal.signal(signal.SIGINT, ignore_signal)

        with open(program_filepath, 'r') as program_file:
            program = BpfProgram(text=program_file.read())
            program.filter_pid(pid)

            # Create and boot event handlers
            handlers = []
            for handler in Tracer.get_handlers():
                handler.boot()
                handlers.append(handler)

            # Create pipes and workers
            (output_stream, input_stream) = multiprocessing.Pipe()
            worker = events.EventProcessor(input_stream, handlers)
            worker.daemon = True
            worker.start()

            # Create event handler and listener worker
            bpf_event_handler = events.handling.BpfEventHandler(output_stream)
            bpf_event_handler.boot()

            bpf_listener_worker = events.bpf.BpfEventListener(program, bpf_event_handler)
            bpf_listener_worker.daemon = True
            bpf_listener_worker.start()
            os.kill(pid, signal.SIGCONT)

            # Wait for the producer to finish
            while bpf_listener_worker.is_alive():
                bpf_listener_worker.join()
            bpf_event_handler.shutdown()

            # Shutdown workers
            print 'Attempting to shutdown worker...'
            output_stream.send('exit')
            worker.join()

            # Shutdown handlers
            print 'Attempting to shutdown handlers...'
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
        # give some time to make sure listener has started
        # seems a little too "hackish"...
        time.sleep(5)
        os.execvp(program[0], program)
    else:
        return pid


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(prog='falcon-tracer', description='This program is the tracer of the Falcon pipeline tool.')

    parser.add_argument('--pid', type=int, nargs='?', default=0,
                        help="filter events of the given PID. (0 = all PIDs)")
    parser.add_argument('--run', nargs='+', default='',
                        metavar=('PROGRAM', 'ARGS'),
                        help="program to run and filter events")
    args = parser.parse_args()
    if args.run != '':
        prog_pid = run_program(args.run)
    else:
        prog_pid = args.pid
    sys.exit(Tracer().run(
        pid=prog_pid))

if __name__ == '__main__':
    main()

import argparse
import sys
from lib import FalconTracer

def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(prog='falcon-agent', description='The tracer of Falcon that collects events of processes')
    parser.add_argument('--pid', type=int, help="filter events of the given PID")
    parser.add_argument('--disable-socket', action='store_true', help='disable socket tracing')
    parser.add_argument('--disable-process', action='store_true', help='disable process lifecycle tracing')
    args = parser.parse_args()

    service = FalconTracer(
                       disable_process_events=args.disable_process,
                       disable_socket_events=args.disable_socket,
                       pid_filter=args.pid if args.pid > 0 else 0,
                       )
    return service.run()

if __name__ == '__main__':
    returned_value = main()

    print "Exiting..."
    sys.exit(returned_value)

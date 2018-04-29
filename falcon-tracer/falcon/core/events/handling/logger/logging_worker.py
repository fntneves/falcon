import multiprocessing
import ujson as json
import signal
from falcon import util


class LoggingWorker(multiprocessing.Process):
    def __init__(self, stream):
        self._stream = stream
        super(LoggingWorker, self).__init__(name='event_logger')

    def run(self):
        exit = False

        while not exit:
            signal.signal(signal.SIGINT, util.ignore_signal)

            try:
                data = self._stream.recv()

                if data == 'exit':
                    exit = True
                else:
                    for event in data:
                        print json.dumps(event)
            except EOFError:
                print 'Cannot read. Stream is closed...'

        self._stream.close()
        print 'Logging worker {} is exiting...'.format(
            str(multiprocessing.current_process().pid))

import multiprocessing
import json
import signal
from falcon import util


class LoggingWorker(multiprocessing.Process):
    def __init__(self, input_stream):
        self._input_stream = input_stream
        super(LoggingWorker, self).__init__(name='event_logger')

    def run(self):
        exit = False

        while not exit:
            signal.signal(signal.SIGINT, util.ignore_signal)

            # Prevent unexpected behavior caused by signals
            event = self._input_stream.get()

            if event == 'exit':
                exit = True
            else:
                print json.dumps(event)

            self._input_stream.task_done()

        print 'Logging worker {} is exiting...'.format(
            str(multiprocessing.current_process().pid))

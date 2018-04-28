import multiprocessing
from event_parser import EventParser
from logging_worker import LoggingWorker
from falcon.core.events.handling.base_handler import BaseHandler

class EventLogger(BaseHandler):
    def __init__(self):
        self._booted = False
        self._output_stream = None
        self._worker = None

    def boot(self):
        (output_stream, input_stream) = multiprocessing.Pipe()
        self._stream = output_stream
        self._worker = LoggingWorker(input_stream)
        self._worker.daemon = True
        self._worker.start()
        self._booted = True

    def handle(self, cpu, data, size):
        assert self._booted is True

        event = EventParser.parse(cpu, data, size)
        if event is None:
            return

        if not isinstance(event, list):
            self._stream.send([event])
        else:
            self._stream.send(event)

    def shutdown(self):
        if self._booted:
            assert self._stream is not None
            assert self._worker is not None
        else:
            return

        self._stream.send('exit')
        self._worker.join()
        self._stream.close()

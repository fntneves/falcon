import logging
import uuid
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.types import SocketSend, SocketReceive, SocketConnect
from falcon.core.events.handling.base_handler import BaseHandler

class FalconEventLogger(BaseHandler):
    def __init__(self, writer):
        self._writer = writer
        super(FalconEventLogger, self).__init__()

    def boot(self):
        logging.info('Booting FalconEventLogger handler...')
        self._writer.open()

    def handle(self, cpu, data, size):
        event = EventFactory.create(data)

        if (isinstance(event, SocketReceive) and event._sport in [9092, 53]) or (isinstance(event, SocketSend) and event._dport in [9092, 53]):
            return

        if (isinstance(event, SocketConnect) and event._dport in [9092, 53]):
            return

        from pprint import pprint
        pprint(event.__dict__)

        self._writer.write(event)

    def shutdown(self):
        logging.info('Shutting down FalconEventLogger handler...')
        self._writer.close()

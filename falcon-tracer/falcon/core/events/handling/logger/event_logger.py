import logging
import ujson as json
import socket
import time
from event_parser import EventParser
from falcon.core.events.handling.base_handler import BaseHandler

class EventLogger(BaseHandler):
    def __init__(self):
        self.logger = logging.getLogger('event')
        self._hostname = socket.getfqdn()

    def handle(self, cpu, data, size):
        data.timestamp = int(round(time.time() * 1000))
        data.host = self._hostname

        data = EventParser.parse(cpu, data, size)
        if data is None:
            return

        if not isinstance(data, list):
            data = [data]

        for event in data:
            self.logger.info(json.dumps(event))

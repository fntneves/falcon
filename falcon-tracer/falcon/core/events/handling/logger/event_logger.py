import ujson as json
from event_parser import EventParser
from falcon.core.events.handling.base_handler import BaseHandler

class EventLogger(BaseHandler):
    def handle(self, cpu, data, size):
        data = EventParser.parse(cpu, data, size)
        if data is None:
            return

        if not isinstance(data, list):
            data = [data]

        for event in data:
            print json.dumps(event)

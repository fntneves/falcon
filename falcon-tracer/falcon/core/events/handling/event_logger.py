import logging
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.handling.base_handler import BaseHandler

class FalconEventLogger(BaseHandler):
    def __init__(self, appender):
        self._appender = appender
        super(BaseHandler, self).__init__()

    def handle(self, cpu, data, size):
        event = EventFactory.create(data)

        if data.type == EventType.PROCESS_CREATE:
            self._appender.append(event)
            self._appender.append(EventFactory.create(data, event_type=EventType.PROCESS_START))
        elif data.type == EventType.PROCESS_JOIN:
            self._appender.append(EventFactory.create(data, event_type=EventType.PROCESS_END))
            self._appender.append(event)
        else:
            self._appender.append(event)

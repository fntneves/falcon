import os
import logging
import uuid
from py2neo import Graph
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.handling.sysgraph import Neo4jGraph, JavaProcessHandler
from falcon.core.events.handling.base_handler import BaseHandler

class SysGraph(BaseHandler):
    def __init__(self):
        self._graph = Neo4jGraph(os.getenv('NEO4J_URI'), os.getenv(
            'NEO4J_USER'), os.getenv('NEO4J_PASSWORD'))

        self._sub_handlers = [
            JavaProcessHandler(os.getenv('JMX_AGENT_HOST'),
                               os.getenv('JMX_AGENT_PORT'))
        ]

        super(SysGraph, self).__init__()

    def boot(self):
        logging.info('Booting SysGraph handler...')
        for handler in self._sub_handlers:
            handler.boot()

    def handle(self, cpu, data, size):
        if not EventType.is_socket(data.type):
            return

        event = EventFactory.create(data)

        if data.type == EventType.SOCKET_ACCEPT:
            self._graph.add_connection()
        elif data.type == EventType.SOCKET_SEND:
            self._graph.update_connection()

    def shutdown(self):
        logging.info('Shutting down SysGraph handler...')
        for handler in self._sub_handlers:
            handler.shutdown()

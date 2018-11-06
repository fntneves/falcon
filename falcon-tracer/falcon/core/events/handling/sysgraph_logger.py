import os
import logging
import uuid
import numa
import ujson
import pprint
import sys
from multiprocessing import Process, Queue
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import SimpleHTTPServer as Handler
from py2neo import Graph
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.handling.sysgraph import Neo4jGraph, JavaProcessHandler
from falcon.core.events.handling.base_handler import BaseHandler

class HTTPServerHandler(BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server):
        BaseHTTPRequestHandler.__init__(self, request, client_address, server)

    def do_GET(self):
        node_count = numa.get_max_node() + 1

        response = {
            'nodes_count': node_count,
            'nodes_info': {},
            'distance_matrix': [[numa.get_distance(i, j) for j in range(node_count)] for i in range(node_count)]
        }

        for i in range(node_count):
            node_size = numa.get_node_size(i)
            response['nodes_info'][i] = {
                'cpus': numa.node_to_cpus(i),
                'memory': {
                    'free': node_size[0],
                    'total': node_size[1],
                }
            }

        HTTPServerHandler.response = response
        contents = ujson.dumps(HTTPServerHandler.response, ensure_ascii=False).encode('utf8')
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Content-Length', len(contents))
        self.end_headers()
        self.wfile.write(contents)

class NUMADetailsHTTPServer(Process):

    def __init__(self, **kwargs):
        super(NUMADetailsHTTPServer, self).__init__()
        self._kwargs = kwargs

    def run(self):
        """Build some CPU-intensive tasks to run via multiprocessing here."""
        try:
            server = HTTPServer(
                ('', 8080), HTTPServerHandler)
            logging.info("NUMA HTTP server is open at http://localhost:8080")
            server.serve_forever()
        except KeyboardInterrupt:
            logging.info("Shutting down the web server")
            server.socket.close()


class SysGraph(BaseHandler):
    def __init__(self):
        self._http = NUMADetailsHTTPServer()
        self._http.start()

        self._graph = Neo4jGraph(os.getenv('NEO4J_URI'), os.getenv(
            'NEO4J_USER'), os.getenv('NEO4J_PASSWORD'))

        self._sub_handlers = [
            JavaProcessHandler(os.getenv('JMX_AGENT_HOST'),
                               os.getenv('JMX_AGENT_PORT'))
        ]

        super(SysGraph, self).__init__()

    def boot(self):
        logging.info('Booting SysGraph handler...')

    def handle(self, cpu, data, size):
        if not EventType.is_socket(data.type):
            return

        event = EventFactory.create(data)
        cpu_affinity = numa.get_affinity(event._pid)

        # Refactor: Move numa info to a dedicated class.
        if len(cpu_affinity) > 4:
            event._cpu_affinity = cpu_affinity

        # Ignore self events
        if event._pid == os.getpid():
            return

        if data.type == EventType.SOCKET_ACCEPT or data.type == EventType.SOCKET_CONNECT:
            self._graph.add_connection(event)
        elif data.type == EventType.SOCKET_SEND or data.type == EventType.SOCKET_RECEIVE:
            self._graph.update_connection(event)
        else:
            # Ignore other events
            return

        for handler in self._sub_handlers:
            handler.handle(event)

    def shutdown(self):
        self._http.join()
        logging.info('Shutting down SysGraph handler...')

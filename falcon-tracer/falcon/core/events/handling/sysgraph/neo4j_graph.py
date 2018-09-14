import os
from py2neo import Graph

class Neo4jGraph(object):
    def __init__(self, uri, user, password):
        self._driver = Graph(uri, user=user, password=password)
        self._driver.run("CREATE CONSTRAINT ON (s:Socket) ASSERT s.socket_id IS UNIQUE")

    def add_connection(self, event):
        self._driver.run(
            "MERGE (h:Host {name: $host}) "
            "MERGE (loc_p:Process {pid: $pid, host: h.name, comm: $comm}) "
            "MERGE (socket:Socket {socket_id: $socket_id}) "
            "ON CREATE SET socket.from = $from_addr, socket.to = $to_addr, socket.created_at = $created_at, socket.bytes = 0 "
            "ON MATCH SET socket.created_at = $created_at "
            "MERGE (h)-[:HAS_PID]-(loc_p) "
            "MERGE (loc_p)-[r:CONNECTED_TO]-(socket) ",
            host=event._host,
            pid=event._pid,
            comm=event._comm,
            socket_id=event._socket_id,
            from_addr=event._socket_from,
            to_addr=event._socket_to,
            created_at=event._timestamp,
        )

    def update_connection(self, event):
        self._driver.run(
            "MERGE (h:Host {name: $host}) "
            "MERGE (loc_p:Process {pid: $pid, host: h.name, comm: $comm}) "
            "MERGE (socket:Socket {socket_id: $socket_id}) "
            "ON CREATE SET socket.from = $from_addr, socket.to = $to_addr, socket.created_at = $created_at, socket.bytes = bytes + $size "
            "ON MATCH SET socket.created_at = $created_at "
            "MERGE (h)-[:HAS_PID]-(loc_p) "
            "MERGE (loc_p)-[r:CONNECTED_TO]-(socket) ",
            host=event._host,
            pid=event._pid,
            comm=event._comm,
            socket_id=event._socket_id,
            from_addr=event._socket_from,
            to_addr=event._socket_to,
            created_at=event._timestamp,
            size=event._size,
        )

    def remove_connection(self, event):
        print "removing connection"


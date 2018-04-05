import socket
import struct
import json
from ... import util
from .. import EventType

class PrinterSocketEventHandler():
    def handle(self, cpu, event):
        if event.type / EventType.SOCKET == 1:
            self.handle_socket_event(cpu, event)
        elif event.type / EventType.PROCESS == 1:
            self.handle_process_event(cpu, event)

    def handle_socket_event(self, cpu, event):
        sock_from = socket.inet_ntop(
            event.data.socket.family, struct.pack("I", event.data.socket.saddr))
        sock_to = socket.inet_ntop(
            event.data.socket.family, struct.pack("I", event.data.socket.daddr))
        sock_id = util.to_socket_id(event.data.socket.saddr, sock_from, event.data.socket.daddr,
                                    sock_to, event.data.socket.sport, event.data.socket.dport)

        if event.type == EventType.SOCKET_CONNECT:
            data = {
                "type": "CONNECT",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_from,
                "src_port": event.data.socket.sport,
                "dst": sock_to,
                "dst_port": event.data.socket.dport,
            }

            print json.dumps(data)
        elif event.type == EventType.SOCKET_ACCEPT:
            data = {
                "type": "ACCEPT",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_to,
                "src_port": event.data.socket.dport,
                "dst": sock_from,
                "dst_port": event.data.socket.sport,
            }

            print json.dumps(data)
        elif event.type == EventType.SOCKET_SEND:
            data = {
                "type": "SND",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_from,
                "src_port": event.data.socket.sport,
                "dst": sock_to,
                "dst_port": event.data.socket.dport,
                "size": event.extra.bytes,
            }

            print json.dumps(data)
        elif event.type == EventType.SOCKET_RECEIVE:
            data = {
                "type": "RCV",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_to,
                "src_port": event.data.socket.dport,
                "dst": sock_from,
                "dst_port": event.data.socket.sport,
                "size": event.extra.bytes,
            }

            print json.dumps(data)

    def handle_process_event(self,cpu, event):
        if event.type == EventType.PROCESS_CREATE:
            data = {
                "type": "CREATE",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "child": event.extra.child_pid,
            }

            print json.dumps(data)

            data = {
                "type": "START",
                "timestamp": event.timestamp,
                "thread": str(event.extra.child_pid),
            }

            print json.dumps(data)
        elif event.type == EventType.PROCESS_END:
            data = {
                "type": "END",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
            }

            print json.dumps(data)
        elif event.type == EventType.PROCESS_JOIN:
            data = {
                "type": "JOIN",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "child": str(event.extra.child_pid),
            }

            print json.dumps(data)
        # TODO.

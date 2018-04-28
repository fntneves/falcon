import socket
import struct
import json
from falcon import util
from falcon.core.events.types import EventType

class EventParser():
    @staticmethod
    def parse(cpu, event, size):
        if event.type / EventType.SOCKET == 1:
            return EventParser._parse_socket_event(cpu, event)
        elif event.type / EventType.PROCESS == 1:
            return EventParser._parse_process_event(cpu, event)

        return None

    @staticmethod
    def _parse_socket_event(cpu, event):
        try:
            sock_from = socket.inet_ntop(
                event.data.socket.family, struct.pack("I", event.data.socket.saddr))
            sock_to = socket.inet_ntop(
                event.data.socket.family, struct.pack("I", event.data.socket.daddr))
            sock_id = util.to_socket_id(event.data.socket.saddr, sock_from, event.data.socket.daddr,
                                        sock_to, event.data.socket.sport, event.data.socket.dport)
        except ValueError:
            return None

        data = None
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

        return data

    @staticmethod
    def _parse_process_event(cpu, event):
        data = None

        if event.type == EventType.PROCESS_CREATE:
            data = [
                {
                    "type": "CREATE",
                    "timestamp": event.timestamp,
                    "thread": str(event.pid),
                    "child": str(event.extra.child_pid),
                },
                {
                    "type": "START",
                    "timestamp": event.timestamp,
                    "thread": str(event.extra.child_pid),
                }
            ]
        elif event.type == EventType.PROCESS_END:
            data = {
                "type": "END",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
            }
        elif event.type == EventType.PROCESS_JOIN:
            data = {
                "type": "JOIN",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "child": str(event.extra.child_pid),
            }

        return data

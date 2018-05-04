import socket
import struct
import logging
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
            if event.socket.family == socket.AF_INET:
                sock_from = socket.inet_ntop(event.socket.family, struct.pack("I", event.socket.saddr[0]))
                sock_to = socket.inet_ntop(event.socket.family, struct.pack("I", event.socket.daddr[0]))
                sock_id = util.to_socket_id(event.socket.saddr[0], sock_from, event.socket.daddr[0], sock_to, event.socket.sport, event.socket.dport)
            elif event.socket.family == socket.AF_INET6:
                sock_from = socket.inet_ntop(event.socket.family, event.socket.saddr)
                sock_to = socket.inet_ntop(event.socket.family, event.socket.daddr)
                sock_id = util.to_socket_id(event.socket.saddr, sock_from, event.socket.daddr,sock_to, event.socket.sport, event.socket.dport)
            else:
                raise ValueError('Undefined socket family: {}' % event.socket_family)
        except ValueError as ve:
            logging.info('Could not generate socket IDs. {}' % ve)
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
                "src_port": event.socket.sport,
                "dst": sock_to,
                "dst_port": event.socket.dport,
            }
        elif event.type == EventType.SOCKET_ACCEPT:
            data = {
                "type": "ACCEPT",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_to,
                "src_port": event.socket.dport,
                "dst": sock_from,
                "dst_port": event.socket.sport,
            }
        elif event.type == EventType.SOCKET_SEND:
            data = {
                "type": "SND",
                "timestamp": event.timestamp,
                "thread": str(event.pid),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_from,
                "src_port": event.socket.sport,
                "dst": sock_to,
                "dst_port": event.socket.dport,
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
                "src_port": event.socket.dport,
                "dst": sock_from,
                "dst_port": event.socket.sport,
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

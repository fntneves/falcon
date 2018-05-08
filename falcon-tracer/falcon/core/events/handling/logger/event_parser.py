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
            sock_from, sock_to, sock_id = EventParser.get_sock_info(
                event.socket.family, event.socket.saddr, event.socket.sport, event.socket.daddr, event.socket.dport)
        except ValueError:
            logging.info('Could not generate socket IDs.')
            return None

        data = None
        if event.type == EventType.SOCKET_CONNECT:
            data = {
                "type": "CONNECT",
                "timestamp": event.timestamp,
                "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_from,
                "src_port": event.socket.sport,
                "dst": sock_to,
                "dst_port": event.socket.dport,
                "data": {
                    "host": event.host,
                }
            }
        elif event.type == EventType.SOCKET_ACCEPT:
            data = {
                "type": "ACCEPT",
                "timestamp": event.timestamp,
                "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_to,
                "src_port": event.socket.dport,
                "dst": sock_from,
                "dst_port": event.socket.sport,
                "data": {
                    "host": event.host,
                }
            }
        elif event.type == EventType.SOCKET_SEND:
            data = {
                "type": "SND",
                "timestamp": event.timestamp,
                "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_from,
                "src_port": event.socket.sport,
                "dst": sock_to,
                "dst_port": event.socket.dport,
                "size": event.extra.bytes,
                "data": {
                    "host": event.host,
                }
            }
        elif event.type == EventType.SOCKET_RECEIVE:
            data = {
                "type": "RCV",
                "timestamp": event.timestamp,
                "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                "socket": sock_id,
                "socket_type": "TCP",
                "src": sock_to,
                "src_port": event.socket.dport,
                "dst": sock_from,
                "dst_port": event.socket.sport,
                "size": event.extra.bytes,
                "data": {
                    "host": event.host,
                }
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
                    "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                    "child": EventParser.thread_id(event.extra.child_pid, event.pid, event.host),
                    "data": {
                        "host": event.host,
                    }
                },
                {
                    "type": "START",
                    "timestamp": event.timestamp,
                    "thread": EventParser.thread_id(event.extra.child_pid, event.pid, event.host),
                    "data": {
                        "host": event.host,
                    }
                }
            ]
        elif event.type == EventType.PROCESS_JOIN:
            data = [
                {
                    "type": "END",
                    "timestamp": event.timestamp,
                    "thread": EventParser.thread_id(event.extra.child_pid, event.pid, event.host),
                    "data": {
                        "host": event.host,
                    }
                },
                {
                    "type": "JOIN",
                    "timestamp": event.timestamp,
                    "thread": EventParser.thread_id(event.pid, event.tgid, event.host),
                    "child": EventParser.thread_id(event.extra.child_pid, event.pid, event.host),
                    "data": {
                        "host": event.host,
                    }
                }
            ]

        return data

    @staticmethod
    def get_sock_info(family, saddr, sport, daddr, dport):
        if family == socket.AF_INET:
            sock_from = socket.inet_ntop(
                socket.AF_INET, struct.pack("I", saddr[0]))
            sock_to = socket.inet_ntop(
                socket.AF_INET, struct.pack("I", daddr[0]))
            sock_id = util.to_socket_id(
                saddr[0], sock_from, daddr[0], sock_to, sport, dport)
        elif family == socket.AF_INET6:
            if (saddr[0] == 0x0 and saddr[1] & 0xffff0000 == 0xffff0000) and (daddr[0] == 0x0 and daddr[1] & 0xffff0000 == 0xffff0000):
                # Look for IPv4 mapped source address
                sock_from = socket.inet_ntop(
                    socket.AF_INET, struct.pack("I", saddr[1] >> 32))
                logging.debug(
                    'Source IPv4 mapped to IPv6 address detected: {}'.format(sock_from))
                sock_to = socket.inet_ntop(
                    socket.AF_INET, struct.pack("I", daddr[1] >> 32))
                logging.debug(
                    'Destination IPv4 mapped to IPv6 address detected: {}'.format(sock_to))

                sock_id = util.to_socket_id(
                    saddr[1] >> 32, sock_from, daddr[1] >> 32, sock_to, sport, dport)
            else:
                sock_from = socket.inet_ntop(socket.AF_INET6, saddr)
                sock_to = socket.inet_ntop(socket.AF_INET6, daddr)
                sock_id = util.to_socket_id(
                    saddr, sock_from, daddr, sock_to, sport, dport)

        else:
            raise ValueError('Undefined socket family: {}'.format(family))

        return (sock_from, sock_to, sock_id)

    @staticmethod
    def thread_id(pid, tgid, host):
        return '{}@{}'.format(pid, host)

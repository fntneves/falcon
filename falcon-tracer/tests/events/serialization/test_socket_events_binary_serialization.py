# -*- coding: utf-8 -*-

import unittest
import struct
import socket
from falcon.core.events.base_event import EventType
from falcon.core.events import types
from falcon.core.events.types.socket_events import SocketEvent


class TestSocketEventsBinarySerialization(unittest.TestCase):
    """Test cases for binary serialization of socket events."""

    def test_base_socket_event_binary_serialization(self):
        """Test binary serialization of a base Socket event."""
        pid = 1000
        tgid = 999
        comm = "java"
        sport = 1024
        dport = 4201
        saddr = [0x0, 0x0100007f]  # 127.0.0.1
        daddr = [0x0, 0x0201A8C0]  # 192.168.1.2
        family = socket.AF_INET
        timestamp = 123456789
        host = "tests"

        event = SocketEvent(pid, tgid, comm, sport, dport,
                            saddr, daddr, family, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! Q I', pack[:12])

        self.assertEquals(timestamp, unpack[0])
        self.assertEquals(len(host), unpack[1])

        unpack = struct.unpack(
            '! {}s I I 16s H QQ QQ H H'.format(unpack[1]), pack[12:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(family, unpack[4])
        self.assertEquals(saddr[0], unpack[5])
        self.assertEquals(saddr[1], unpack[6])
        self.assertEquals(daddr[0], unpack[7])
        self.assertEquals(daddr[1], unpack[8])
        self.assertEquals(sport, unpack[9])
        self.assertEquals(dport, unpack[10])

    def test_socket_connect_event_binary_serialization(self):
        """Test binary serialization of a Socket Connect event."""
        pid = 1000
        tgid = 999
        comm = "java"
        sport = 1024
        dport = 4201
        saddr = [0x0, 0x0100007f]  # 127.0.0.1
        daddr = [0x0, 0x0201A8C0]  # 192.168.1.2
        family = socket.AF_INET
        timestamp = 123456789
        host = "tests"

        event = types.SocketConnect(
            pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.SOCKET_CONNECT, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack(
            '! {}s I I 16s H QQ QQ H H'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(family, unpack[4])
        self.assertEquals(saddr[0], unpack[5])
        self.assertEquals(saddr[1], unpack[6])
        self.assertEquals(daddr[0], unpack[7])
        self.assertEquals(daddr[1], unpack[8])
        self.assertEquals(sport, unpack[9])
        self.assertEquals(dport, unpack[10])

    def test_socket_accept_event_binary_serialization(self):
        """Test binary serialization of a Socket Accept event."""
        pid = 1000
        tgid = 999
        comm = "java"
        sport = 1024
        dport = 4201
        saddr = [0x0, 0x0100007f]  # 127.0.0.1
        daddr = [0x0, 0x0201A8C0]  # 192.168.1.2
        family = socket.AF_INET
        timestamp = 123456789
        host = "tests"

        event = types.SocketAccept(
            pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.SOCKET_ACCEPT, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack(
            '! {}s I I 16s H QQ QQ H H'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(family, unpack[4])
        self.assertEquals(saddr[0], unpack[5])
        self.assertEquals(saddr[1], unpack[6])
        self.assertEquals(daddr[0], unpack[7])
        self.assertEquals(daddr[1], unpack[8])
        self.assertEquals(sport, unpack[9])
        self.assertEquals(dport, unpack[10])

    def test_socket_send_event_binary_serialization(self):
        """Test binary serialization of a Socket Send event."""
        pid = 1000
        tgid = 999
        comm = "java"
        sport = 1024
        dport = 4201
        saddr = [0x0, 0x0100007f]  # 127.0.0.1
        daddr = [0x0, 0x0201A8C0]  # 192.168.1.2
        family = socket.AF_INET
        size = 100
        timestamp = 123456789
        host = "tests"

        event = types.SocketSend(
            pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.SOCKET_SEND, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack(
            '! {}s I I 16s H QQ QQ H H I'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(family, unpack[4])
        self.assertEquals(saddr[0], unpack[5])
        self.assertEquals(saddr[1], unpack[6])
        self.assertEquals(daddr[0], unpack[7])
        self.assertEquals(daddr[1], unpack[8])
        self.assertEquals(sport, unpack[9])
        self.assertEquals(dport, unpack[10])
        self.assertEquals(size, unpack[11])

    def test_socket_receive_event_binary_serialization(self):
        """Test binary serialization of a Socket Receive event."""
        pid = 1000
        tgid = 999
        comm = "java"
        sport = 1024
        dport = 4201
        saddr = [0x0, 0x0100007f]  # 127.0.0.1
        daddr = [0x0, 0x0201A8C0]  # 192.168.1.2
        family = socket.AF_INET
        size = 100
        timestamp = 123456789
        host = "tests"

        event = types.SocketReceive(
            pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.SOCKET_RECEIVE, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack(
            '! {}s I I 16s H QQ QQ H H I'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(family, unpack[4])
        self.assertEquals(saddr[0], unpack[5])
        self.assertEquals(saddr[1], unpack[6])
        self.assertEquals(daddr[0], unpack[7])
        self.assertEquals(daddr[1], unpack[8])
        self.assertEquals(sport, unpack[9])
        self.assertEquals(dport, unpack[10])
        self.assertEquals(size, unpack[11])


if __name__ == '__main__':
    unittest.main()

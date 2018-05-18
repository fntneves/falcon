from falcon.core.events.base_event import Event, EventType
import ujson as json
import socket
import struct
import logging


class SocketEvent(Event):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp=None, host=None):
        self._sport = sport
        self._dport = dport
        self._saddr = saddr
        self._daddr = daddr
        self._family = family
        self._type = socket.SOCK_STREAM
        self._socket_from = None
        self._socket_to = None
        self._socket_id = None
        self._fill_socket_data()
        super(SocketEvent, self).__init__(pid, tgid, comm, timestamp, host)

    def _fill_socket_data(self):
        sock_saddr = self._saddr
        sock_daddr = self._daddr

        # Handle IPv4 sockets
        if self._family == socket.AF_INET:
            sock_saddr = self._saddr[1]
            sock_daddr = self._daddr[1]
            sock_from = socket.inet_ntop(socket.AF_INET, struct.pack("I", sock_saddr))
            sock_to = socket.inet_ntop(socket.AF_INET, struct.pack("I", sock_daddr))

            if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
                logging.debug('ipv4 saddr: {} -> {}'.format(hex(sock_saddr), sock_from))
                logging.debug('ipv4 daddr: {} -> {}'.format(hex(sock_daddr), sock_to))
        # Handle IPv6 sockets
        elif self._family == socket.AF_INET6:
            # Handle IPv4-mapped IPv6 source socket addresses
            if self._saddr[0] == 0x0 and self._saddr[1] & 0xffff0000 == 0xffff0000:
                sock_saddr = self._saddr[1] >> 32
                sock_from = socket.inet_ntop(socket.AF_INET, struct.pack("I", sock_saddr))

                if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
                    logging.debug('ipv4-mapped saddr: {} -> {}'.format(hex(sock_saddr), sock_from))
            else:
                sock_from = socket.inet_ntop(socket.AF_INET6, self._saddr)

            # Handle IPv4-mapped IPv6 destination socket addresses
            if self._daddr[0] == 0x0 and self._daddr[1] & 0xffff0000 == 0xffff0000:
                # Convert IPv4-mapped destination IPv6 address to IPv4
                sock_daddr = self._daddr[1] >> 32
                sock_to = socket.inet_ntop(socket.AF_INET, struct.pack("I", sock_daddr))

                if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
                    logging.debug('ipv4-mapped daddr: {} -> {}'.format(hex(sock_daddr), sock_from))
            else:
                sock_to = socket.inet_ntop(socket.AF_INET6, self._daddr)

        else:
            raise ValueError(
                'Undefined socket family: {}'.format(self._family))

        # Generate socket id
        self._socket_from = sock_from
        self._socket_to = sock_to
        self._socket_id = SocketEvent._generate_socket_id(
            sock_daddr, sock_from, sock_daddr, sock_to, self._sport, self._dport)

    def to_bytes(self):
        base_data = super(SocketEvent, self).to_bytes()
        data = (
            base_data,
            self._family,
            self._saddr[0],
            self._saddr[1],
            self._daddr[0],
            self._daddr[1],
            self._sport,
            self._dport
        )

        struct_format = "! {}s H QQ QQ H H".format(len(base_data))

        return struct.pack(struct_format, *data)

    @staticmethod
    def _generate_socket_id(addr1, addr1_str, addr2, addr2_str, port1, port2):
        socket_id = None
        if addr1 < addr2:
            socket_id = "%s:%d-%s:%d" % (addr1_str, port1, addr2_str, port2)
        elif addr2 < addr1:
            socket_id = "%s:%d-%s:%d" % (addr2_str, port2, addr1_str, port1)
        else:
            if port1 < port2:
                socket_id = "%s:%d-%s:%d" % (addr1_str, port1, addr2_str, port2)
            else:
                socket_id = "%s:%d-%s:%d" % (addr2_str, port2, addr1_str, port1)

        return socket_id


class SocketConnect(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp=None, host=None):
        super(SocketConnect, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": EventType.SOCKET_CONNECT,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "data": {
                    "host": self._host,
            }
        })

    def to_bytes(self):
        base_data = super(SocketConnect, self).to_bytes()
        data = (EventType.SOCKET_CONNECT, base_data)

        struct_format = "! B {}s".format(len(base_data))

        return struct.pack(struct_format, *data)


class SocketAccept(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp=None, host=None):
        super(SocketAccept, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": "ACCEPT",
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "data": {
                    "host": self._host,
            }
        })

    def to_bytes(self):
        base_data = super(SocketAccept, self).to_bytes()
        data = (EventType.SOCKET_ACCEPT, base_data)

        struct_format = "! B {}s".format(len(base_data))

        return struct.pack(struct_format, *data)


class SocketSend(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp=None, host=None):
        self._size = size
        super(SocketSend, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": "SND",
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "size": self._size,
            "data": {
                    "host": self._host,
            }
        })

    def to_bytes(self):
        base_data = super(SocketSend, self).to_bytes()
        data = (EventType.SOCKET_SEND, base_data, self._size)

        struct_format = "! B {}s I".format(len(base_data))

        return struct.pack(struct_format, *data)


class SocketReceive(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp=None, host=None):
        self._size = size
        super(SocketReceive, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": "SND",
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "size": self._size,
            "data": {
                    "host": self._host,
            }
        })

    def to_bytes(self):
        base_data = super(SocketReceive, self).to_bytes()
        data = (EventType.SOCKET_RECEIVE, base_data, self._size)

        struct_format = "! B {}s I".format(len(base_data))

        return struct.pack(struct_format, *data)

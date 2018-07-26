from falcon.core.events.base_event import Event, EventType
import ujson as json
import socket
import struct
import logging
import falcon.core.protocol.fbs.FalconEvent as FlatFalconEvent
import falcon.core.protocol.fbs.SocketEvent as FlatSocketEvent
import falcon.core.protocol.fbs.SocketAccept as FlatSocketAccept
import falcon.core.protocol.fbs.SocketConnect as FlatSocketConnect
import falcon.core.protocol.fbs.SocketSend as FlatSocketSend
import falcon.core.protocol.fbs.SocketReceive as FlatSocketReceive
import flatbuffers



class SocketEvent(Event):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp=None, host=None):
        self._sport = sport
        self._dport = dport
        self._saddr = saddr
        self._daddr = daddr
        self._family = family
        self._socket_type = socket.SOCK_STREAM
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
        self._type = EventType.SOCKET_CONNECT
        super(SocketConnect, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": self._type,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._socket_type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)
        socket_from_field = builder.CreateString(self._socket_from)
        socket_to_field = builder.CreateString(self._socket_to)
        socket_id_field = builder.CreateString(self._socket_id)

        # Create SocketConnect event
        FlatSocketConnect.SocketConnectStart(builder)
        event_data = FlatSocketConnect.SocketConnectEnd(builder)

        # Create SocketEvent event
        FlatSocketEvent.SocketEventStart(builder)
        FlatSocketEvent.SocketEventAddSourcePort(builder, self._sport)
        FlatSocketEvent.SocketEventAddDestinationPort(builder, self._dport)
        FlatSocketEvent.SocketEventAddSocketFamily(builder, self._family)
        FlatSocketEvent.SocketEventAddSocketType(builder, self._socket_type)
        FlatSocketEvent.SocketEventAddSocketFrom(builder, socket_from_field)
        FlatSocketEvent.SocketEventAddSocketTo(builder, socket_to_field)
        FlatSocketEvent.SocketEventAddSocketId(builder, socket_id_field)
        FlatSocketEvent.SocketEventAddEvent(builder, event_data)
        socket_event_data = FlatSocketEvent.SocketEventEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, self._type)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, socket_event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


class SocketAccept(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp=None, host=None):
        self._type = EventType.SOCKET_ACCEPT
        super(SocketAccept, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": self._type,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._socket_type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)
        socket_from_field = builder.CreateString(self._socket_from)
        socket_to_field = builder.CreateString(self._socket_to)
        socket_id_field = builder.CreateString(self._socket_id)

        # Create SocketAccept event
        FlatSocketAccept.SocketAcceptStart(builder)
        event_data = FlatSocketAccept.SocketAcceptEnd(builder)

        # Create SocketEvent event
        FlatSocketEvent.SocketEventStart(builder)
        FlatSocketEvent.SocketEventAddSourcePort(builder, self._sport)
        FlatSocketEvent.SocketEventAddDestinationPort(builder, self._dport)
        FlatSocketEvent.SocketEventAddSocketFamily(builder, self._family)
        FlatSocketEvent.SocketEventAddSocketType(builder, self._socket_type)
        FlatSocketEvent.SocketEventAddSocketFrom(builder, socket_from_field)
        FlatSocketEvent.SocketEventAddSocketTo(builder, socket_to_field)
        FlatSocketEvent.SocketEventAddSocketId(builder, socket_id_field)
        FlatSocketEvent.SocketEventAddEvent(builder, event_data)
        socket_event_data = FlatSocketEvent.SocketEventEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, self._type)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, socket_event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


class SocketSend(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp=None, host=None):
        self._type = EventType.SOCKET_SEND
        self._size = size
        super(SocketSend, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": self._type,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._socket_type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "size": self._size,
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)
        socket_from_field = builder.CreateString(self._socket_from)
        socket_to_field = builder.CreateString(self._socket_to)
        socket_id_field = builder.CreateString(self._socket_id)

        # Create SocketSend event
        FlatSocketSend.SocketSendStart(builder)
        FlatSocketSend.SocketSendAddSize(builder, self._size)
        event_data = FlatSocketSend.SocketSendEnd(builder)

        # Create SocketEvent event
        FlatSocketEvent.SocketEventStart(builder)
        FlatSocketEvent.SocketEventAddSourcePort(builder, self._sport)
        FlatSocketEvent.SocketEventAddDestinationPort(builder, self._dport)
        FlatSocketEvent.SocketEventAddSocketFamily(builder, self._family)
        FlatSocketEvent.SocketEventAddSocketType(builder, self._socket_type)
        FlatSocketEvent.SocketEventAddSocketFrom(builder, socket_from_field)
        FlatSocketEvent.SocketEventAddSocketTo(builder, socket_to_field)
        FlatSocketEvent.SocketEventAddSocketId(builder, socket_id_field)
        FlatSocketEvent.SocketEventAddEvent(builder, event_data)
        socket_event_data = FlatSocketEvent.SocketEventEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, self._type)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, socket_event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


class SocketReceive(SocketEvent):
    def __init__(self, pid, tgid, comm, sport, dport, saddr, daddr, family, size, timestamp=None, host=None):
        self._type = EventType.SOCKET_RECEIVE
        self._size = size
        super(SocketReceive, self).__init__(pid, tgid, comm, sport, dport, saddr, daddr, family, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": self._type,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "socket": self._socket_id,
            "socket_type": "TCP" if self._socket_type == socket.SOCK_STREAM else "UDP",
            "src": self._socket_from,
            "src_port": self._sport,
            "dst": self._socket_to,
            "dst_port": self._dport,
            "size": self._size,
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)
        socket_from_field = builder.CreateString(self._socket_from)
        socket_to_field = builder.CreateString(self._socket_to)
        socket_id_field = builder.CreateString(self._socket_id)

        # Create SocketReceive event
        FlatSocketReceive.SocketReceiveStart(builder)
        FlatSocketReceive.SocketReceiveAddSize(builder, self._size)
        event_data = FlatSocketReceive.SocketReceiveEnd(builder)

        # Create SocketEvent event
        FlatSocketEvent.SocketEventStart(builder)
        FlatSocketEvent.SocketEventAddSourcePort(builder, self._sport)
        FlatSocketEvent.SocketEventAddDestinationPort(builder, self._dport)
        FlatSocketEvent.SocketEventAddSocketFamily(builder, self._family)
        FlatSocketEvent.SocketEventAddSocketType(builder, self._socket_type)
        FlatSocketEvent.SocketEventAddSocketFrom(builder, socket_from_field)
        FlatSocketEvent.SocketEventAddSocketTo(builder, socket_to_field)
        FlatSocketEvent.SocketEventAddSocketId(builder, socket_id_field)
        FlatSocketEvent.SocketEventAddEvent(builder, event_data)
        socket_event_data = FlatSocketEvent.SocketEventEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, self._type)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, socket_event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()

import ctypes
import struct
import socket
import time

TASK_COMM_LEN = 16  # linux/sched.h


class EventType():
    SOCKET_SEND = 8
    SOCKET_RECEIVE = 9
    SOCKET_CONNECT = 11
    SOCKET_ACCEPT = 12

    PROCESS_CREATE = 1
    PROCESS_START = 2
    PROCESS_END = 3
    PROCESS_JOIN = 4

    @staticmethod
    def is_socket(type):
        return type == EventType.SOCKET_SEND or \
            type == EventType.SOCKET_RECEIVE or \
            type == EventType.SOCKET_CONNECT or \
            type == EventType.SOCKET_ACCEPT

    @staticmethod
    def is_process(type):
        return type == EventType.PROCESS_CREATE or \
            type == EventType.PROCESS_START or \
            type == EventType.PROCESS_END or \
            type == EventType.PROCESS_JOIN


class ExtraData(ctypes.Union):
    _fields_ = [
        ("bytes", ctypes.c_uint),
        ("child_pid", ctypes.c_uint),
    ]


class SocketData(ctypes.Structure):
    _fields_ = [
        ("sport", ctypes.c_ushort),
        ("dport", ctypes.c_ushort),
        ("saddr", (ctypes.c_ulonglong * 2)),
        ("daddr", (ctypes.c_ulonglong * 2)),
        ("family", ctypes.c_ushort),
        ("type", ctypes.c_ushort),
    ]


class EventData(ctypes.Structure):
    _fields_ = [
        ("type", ctypes.c_uint),
        ("pid", ctypes.c_uint),
        ("tgid", ctypes.c_uint),
        ("comm", ctypes.c_char * TASK_COMM_LEN),
        ("socket", SocketData),
        ("extra", ExtraData)
    ]

class Event(object):
    hostname = socket.getfqdn()
    def __init__(self, pid, tgid, comm, timestamp=None, host=None):
        self._timestamp = timestamp
        self._host = host

        if self._timestamp is None:
            self._timestamp = int(round(time.time() * 1000))

        if self._host is None:
            self._host = Event.hostname

        self._tid = pid
        self._pid = tgid
        self._comm = comm

    def get_thread_id(self):
        return self._generate_thread_id(self._tid, self._host)

    def to_json(self):
        return NotImplemented

    def to_bytes(self):
        data = (self._timestamp, len(self._host), self._host, self._tid, self._pid, self._comm)

        return struct.pack('! Q I{}s I I 16s'.format(len(self._host)), *data)

    def _generate_thread_id(self, pid, host):
        return '{}@{}'.format(pid, host)

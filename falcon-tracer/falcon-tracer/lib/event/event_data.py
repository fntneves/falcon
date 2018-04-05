import ctypes
from .socket_event_data import SocketEventData

TASK_COMM_LEN = 16 # linux/sched.h

class EventType():
    SOCKET = 100
    SOCKET_CONNECT = 101
    SOCKET_ACCEPT = 102
    SOCKET_SEND = 103
    SOCKET_RECEIVE = 104

    PROCESS = 200
    PROCESS_START = 201
    PROCESS_END = 202
    PROCESS_CREATE = 203
    PROCESS_JOIN = 204

class DataUnion(ctypes.Union):
    _fields_ = [
        ("socket", SocketEventData),
    ]


class ExtraDataUnion(ctypes.Union):
    _fields_ = [
        ("bytes", ctypes.c_uint),
        ("child_pid", ctypes.c_uint),
    ]

class EventData(ctypes.Structure):
    _fields_ = [
        ("type", ctypes.c_uint),
        ("timestamp", ctypes.c_ulonglong),
        ("pid", ctypes.c_uint),
        ("tgid", ctypes.c_uint),
        ("ppid", ctypes.c_uint),
        ("comm", ctypes.c_char * TASK_COMM_LEN),
        ("data", DataUnion),
        ("extra", ExtraDataUnion)
    ]

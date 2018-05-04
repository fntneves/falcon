import ctypes

class SocketEvent(ctypes.Structure):
    _fields_ = [
        ("sport", ctypes.c_ushort),
        ("dport", ctypes.c_ushort),
        ("saddr", (ctypes.c_ulonglong * 2)),
        ("daddr", (ctypes.c_ulonglong * 2)),
        ("family", ctypes.c_short),
    ]

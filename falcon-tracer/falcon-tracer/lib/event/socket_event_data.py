import ctypes

class SocketEventData(ctypes.Structure):
    _fields_ = [
        ("sport", ctypes.c_ushort),
        ("dport", ctypes.c_ushort),
        ("saddr", ctypes.c_uint),
        ("daddr", ctypes.c_uint),
        ("family", ctypes.c_short),
    ]

import ctypes
import ctypes.util

libc = ctypes.CDLL(ctypes.util.find_library('c'))

# Get network device's name
def if_indextoname (index):
    if not isinstance (index, int):
        raise TypeError ('Index must be an integer.')
    libc.if_indextoname.argtypes = [ctypes.c_uint32, ctypes.c_char_p]
    libc.if_indextoname.restype = ctypes.c_char_p

    ifname = ctypes.create_string_buffer(32)
    ifname = libc.if_indextoname (index, ifname)
    if not ifname:
        raise RuntimeError ("Invalid network interface index.")
    return ifname

# Generate socket id
def to_socket_id (addr1, addr1_str, addr2, addr2_str, port1, port2):
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

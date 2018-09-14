import socket
import struct


class JavaProcessHandler():
    def __init__(self, host, port):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._sock.connect((host, int(port)))

    def handle(self, event):
        pid_binary = struct.pack('!i', event._pid)
        self._sock.send(pid_binary)

    def __del__(self):
        if (self._sock is not None):
            self._sock.close()

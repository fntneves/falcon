import socket
import struct


class JavaProcessHandler():
    def __init__(self, host, port):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._sock.connect((host, int(port))

    def __del__(self):
        if (self._sock is not None):
            self._sock.close()

    def handle(self, cpu, event):
        pid_binary = struct.pack('!i', event.pid)
        self._sock.send(pid_binary)

import socket
import struct


class JavaProcessHandler():
    def __init__(self, host, port):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._sock.connect((host, port))

    def close(self):
        self._sock.close()

    def handle(self, cpu, event):
        pid_binary = struct.pack('!i', event.pid)
        self._sock.send(pid_binary)

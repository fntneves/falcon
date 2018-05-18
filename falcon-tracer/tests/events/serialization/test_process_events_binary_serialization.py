# -*- coding: utf-8 -*-

import unittest
import struct
from falcon.core.events.base_event import EventType
from falcon.core.events import types


class TestProcessEventsBinarySerialization(unittest.TestCase):
    """Test cases for binary serialization of process events."""

    def test_process_create_binary_serialization(self):
        """Test binary serialization of a Process Create event."""
        pid = 1000
        tgid = 999
        comm = "java"
        child_pid = 1001
        timestamp = 123456789
        host = "tests"

        event = types.ProcessCreate(pid, tgid, comm, child_pid, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.PROCESS_CREATE, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack('! {}s I I 16s I'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(child_pid, unpack[4])

    def test_process_join_binary_serialization(self):
        """Test binary serialization of a Process Join event."""
        pid = 1000
        tgid = 999
        comm = "java"
        child_pid = 1001
        timestamp = 123456789
        host = "tests"

        event = types.ProcessJoin(pid, tgid, comm, child_pid, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.PROCESS_CREATE, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack('! {}s I I 16s I'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])
        self.assertEquals(child_pid, unpack[4])

    def test_process_start_binary_serialization(self):
        """Test binary serialization of a Process Start event."""
        pid = 1000
        tgid = 999
        comm = "java"
        timestamp = 123456789
        host = "tests"

        event = types.ProcessStart(pid, tgid, comm, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.PROCESS_CREATE, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack('! {}s I I 16s'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])

    def test_process_end_binary_serialization(self):
        """Test binary serialization of a Process End event."""
        pid = 1000
        tgid = 999
        comm = "java"
        timestamp = 123456789
        host = "tests"

        event = types.ProcessEnd(pid, tgid, comm, timestamp, host)
        pack = event.to_bytes()
        unpack = struct.unpack('! B Q I', pack[:13])

        self.assertEquals(EventType.PROCESS_CREATE, unpack[0])
        self.assertEquals(timestamp, unpack[1])
        self.assertEquals(len(host), unpack[2])

        unpack = struct.unpack('! {}s I I 16s'.format(unpack[2]), pack[13:])

        self.assertEquals(host, unpack[0])
        self.assertEquals(pid, unpack[1])
        self.assertEquals(tgid, unpack[2])
        self.assertEquals(comm, unpack[3][:unpack[3].index("\0")])

if __name__ == '__main__':
    unittest.main()

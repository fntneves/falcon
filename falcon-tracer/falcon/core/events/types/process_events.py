import struct
import ujson as json
from falcon.core.events.base_event import Event, EventType


class ProcessCreate(Event):
    def __init__(self, pid, tgid, comm, child_pid, timestamp=None, host=None):
        self._child_pid = child_pid
        super(ProcessCreate, self).__init__(pid, tgid, comm, timestamp, host)

    def get_child_thread_id(self):
        return self._generate_thread_id(self._child_pid, self._host)

    def to_json(self):
        return json.dumps({
            "type": EventType.PROCESS_CREATE,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "child": self.get_child_thread_id(),
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        base_data = super(ProcessCreate, self).to_bytes()
        data = (EventType.PROCESS_CREATE, base_data, self._child_pid)

        struct_format = "! B {}s I".format(len(base_data))

        return struct.pack(struct_format, *data)


class ProcessJoin(Event):
    def __init__(self, pid, tgid, comm, child_pid, timestamp=None, host=None):
        self._child_pid = child_pid
        super(ProcessJoin, self).__init__(pid, tgid, comm, timestamp, host)

    def get_child_thread_id(self):
        return self._generate_thread_id(self._child_pid, self._host)

    def to_json(self):
        return json.dumps({
            "type": EventType.PROCESS_JOIN,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "child": self.get_child_thread_id(),
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        base_data = super(ProcessJoin, self).to_bytes()
        data = (EventType.PROCESS_CREATE, base_data, self._child_pid)

        struct_format = "! B {}s I".format(len(base_data))

        return struct.pack(struct_format, *data)


class ProcessStart(Event):
    def __init__(self, pid, tgid, comm, timestamp=None, host=None):
        super(ProcessStart, self).__init__(pid, tgid, comm, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": EventType.PROCESS_START,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        base_data = super(ProcessStart, self).to_bytes()
        data = (EventType.PROCESS_CREATE, base_data)

        struct_format = "! B {}s".format(len(base_data))

        return struct.pack(struct_format, *data)


class ProcessEnd(Event):
    def __init__(self, pid, tgid, comm, timestamp=None, host=None):
        super(ProcessEnd, self).__init__(pid, tgid, comm, timestamp, host)

    def to_json(self):
        return json.dumps({
            "type": EventType.PROCESS_END,
            "timestamp": self._timestamp,
            "thread": self.get_thread_id(),
            "data": {
                "host": self._host,
                "comm": self._comm,
            }
        })

    def to_bytes(self):
        base_data = super(ProcessEnd, self).to_bytes()
        data = (EventType.PROCESS_CREATE, base_data)

        struct_format = "! B {}s".format(len(base_data))

        return struct.pack(struct_format, *data)

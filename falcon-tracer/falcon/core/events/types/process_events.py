import struct
import ujson as json
from falcon.core.events.base_event import Event, EventType
import falcon.core.protocol.fbs.FalconEvent as FlatFalconEvent
import falcon.core.protocol.fbs.ProcessCreate as FlatProcessCreate
import falcon.core.protocol.fbs.ProcessJoin as FlatProcessJoin
import falcon.core.protocol.fbs.ProcessStart as FlatProcessStart
import falcon.core.protocol.fbs.ProcessEnd as FlatProcessEnd
import flatbuffers


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
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)

        # Create ProcessJoin event
        FlatProcessJoin.ProcessJoinStart(builder)
        FlatProcessJoin.ProcessJoinAddChildPid(builder, self._child_pid)
        event_data = FlatProcessJoin.ProcessJoinEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, EventType.PROCESS_CREATE)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


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
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)

        # Create ProcessJoin event
        FlatProcessJoin.ProcessJoinStart(builder)
        FlatProcessJoin.ProcessJoinAddChildPid(builder, self._child_pid)
        event_data = FlatProcessJoin.ProcessJoinEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, EventType.PROCESS_JOIN)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


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
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)

        # Create ProcessStart event
        FlatProcessStart.ProcessStartStart(builder)
        event_data = FlatProcessStart.ProcessStartEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, EventType.PROCESS_START)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()


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
        builder = flatbuffers.Builder(0)
        id_field = builder.CreateString(self._id)
        comm_field = builder.CreateString(self._comm)
        host_field = builder.CreateString(self._host)

        # Create ProcessEnd event
        FlatProcessEnd.ProcessEndStart(builder)
        event_data = FlatProcessEnd.ProcessEndEnd(builder)

        # Create FalconEvent
        FlatFalconEvent.FalconEventStart(builder)
        FlatFalconEvent.FalconEventAddId(builder, id_field)
        FlatFalconEvent.FalconEventAddUserTime(builder, self._timestamp)
        FlatFalconEvent.FalconEventAddKernelTime(builder, self._ktime)
        FlatFalconEvent.FalconEventAddType(builder, EventType.PROCESS_END)
        FlatFalconEvent.FalconEventAddPid(builder, self._pid)
        FlatFalconEvent.FalconEventAddTid(builder, self._tid)
        FlatFalconEvent.FalconEventAddComm(builder, comm_field)
        FlatFalconEvent.FalconEventAddHost(builder, host_field)
        FlatFalconEvent.FalconEventAddEvent(builder, event_data)
        builder.Finish(FlatFalconEvent.FalconEventEnd(builder))

        return builder.Output()

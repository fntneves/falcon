# automatically generated by the FlatBuffers compiler, do not modify

# namespace: fbs

import flatbuffers

class SocketSend(object):
    __slots__ = ['_tab']

    @classmethod
    def GetRootAsSocketSend(cls, buf, offset):
        n = flatbuffers.encode.Get(flatbuffers.packer.uoffset, buf, offset)
        x = SocketSend()
        x.Init(buf, n + offset)
        return x

    # SocketSend
    def Init(self, buf, pos):
        self._tab = flatbuffers.table.Table(buf, pos)

    # SocketSend
    def Size(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            return self._tab.Get(flatbuffers.number_types.Uint32Flags, o + self._tab.Pos)
        return 0

def SocketSendStart(builder): builder.StartObject(1)
def SocketSendAddSize(builder, size): builder.PrependUint32Slot(0, size, 0)
def SocketSendEnd(builder): return builder.EndObject()
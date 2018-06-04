import os


class BinaryWriter(object):
    def __init__(self, file):
        self._file = None
        self._filename = file

    def open(self):
        self._file = open(self._filename, "wb")

    def write(self, event):
        assert self._file is not None
        self._file.write(event.to_bytes())

    def close(self):
        self._file.flush()
        self._file.close()

class JsonWriter(BinaryWriter):
    def __init__(self, file):
        super(JsonWriter, self).__init__(file)

    def write(self, event):
        assert self._file is not None
        self._file.write(event.to_json() + os.linesep)

class BinaryWriter:
    def __init__(self, file):
        self._file = open(file, "wb")

    def append(self, event):
        self._file.write(event.to_bytes())

    def __del__(self):
        self._file.flush()
        self._file.close()

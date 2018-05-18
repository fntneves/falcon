import logging

class JsonWriter:
    def __init__(self):
        self.logger = logging.getLogger('event')

    def append(self, event):
        self.logger.info(event.to_json())

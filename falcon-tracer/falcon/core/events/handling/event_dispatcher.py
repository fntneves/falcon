import logging
import multiprocessing
from sortedcontainers import SortedSet

class EventDispatcher():

    def __init__(self):
        self._channel_timestamp = {}

        ktime = lambda event: event._ktime
        self._events = SortedSet(key=ktime)

        cpu_count = multiprocessing.cpu_count()
        for i in range(cpu_count):
            self._channel_timestamp[i] = 0

    def put(self, channel, data):
        event = EventChannelData(channel, data)

        self._channel_timestamp[channel] = event._ktime
        self._events.add(event)

    def get_remaining(self):
        return self._events

    def retrieve_dispatch_candidates(self):
        min_channel = min(self._channel_timestamp, key=self._channel_timestamp.get)
        min_timestamp = self._channel_timestamp[min_channel]

        if min_timestamp < 1:
            return []

        events = []
        for event in self._events:
            if event._ktime <= min_timestamp:
                events.append(event)

        for event in events:
            self._events.remove(event)

        return events


class EventChannelData:
    def __init__(self, channel, event):
        self.channel = channel
        self.event = event

    def __getattr__(self, attr):
        return getattr(self.event, attr)

    def __getitem__(self, attr):
        return getattr(self.event, attr)

    def __eq__(self, other):
        return self.event._id == other.event._id

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.event._id)
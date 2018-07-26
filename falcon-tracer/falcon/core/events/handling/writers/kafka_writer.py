import os
import logging
import hashlib
import json
from kafka import SimpleClient as KafkaClient
from confluent_kafka import Producer
from falcon.core.events.base_event import EventType

class KafkaWriter:
    def __init__(self, servers):
        self._servers = servers
        self._topics = ['socket_events', 'process_events']
        self._client = None
        self._partitions_count = {}

    def open(self):
        self._boot_topics()
        self._producer = Producer({'bootstrap.servers': self._servers})

    def write(self, event):
        self._producer.poll(0)

        # Asynchronously produce a message, the delivery report callback will
        # will be triggered (from poll or flush), when the message has
        # been successfully delivered or failed permanently.
        topic = self.topic_for_event(event)

        self._producer.produce(topic['name'], buffer(event.to_bytes()), partition=topic['partition'], callback=KafkaWriter.delivery_report)

    def close(self):
        self._producer.flush()
        self._client.close()

    def topic_for_event(self, event):
        topic = None
        key = None

        if EventType.is_process(event._type):
            topic = 'process_events'
            key = event._host
        elif EventType.is_socket(event._type):
            topic = 'socket_events'
            key = event._socket_id
        else:
            raise ValueError('Event type ['+event._type+'] is invalid.')

        return {
            'name': topic,
            'partition': int(hashlib.sha512(key).hexdigest(), 16) % self._partitions_count[topic]
        }

    def _boot_topics(self):
        self._client = KafkaClient(self._servers)

        for topic in self._topics:
            if not self._client.has_metadata_for_topic(topic):
                raise IOError('Kafka topic ['+topic+'] was not found.')

            topic_partitions_count = len(
                self._client.get_partition_ids_for_topic(topic))

            if topic_partitions_count == 0:
                raise IOError('Kafka topic ['+topic+'] does not have any partition.')

            self._partitions_count[topic] = topic_partitions_count

        if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            logging.debug('Booted topics and partitions: ' + json.dumps(self._partitions_count))

    @staticmethod
    def delivery_report(err, msg):
        if err is not None:
            logging.error('Event delivery failed: {}'.format(err))
        elif logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            logging.debug('Event delivered to {} [{}]'.format(
                msg.topic(), msg.partition()))

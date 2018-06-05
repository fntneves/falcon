import os
import logging
import hashlib
from kafka import SimpleClient as KafkaClient
from confluent_kafka import Producer

class KafkaWriter:
    def __init__(self, servers, topic):
        self._servers = servers
        self._topic = topic
        self._client = None
        self._partitions_count = 0

    def open(self):
        self._boot_topic()
        self._producer = Producer({'bootstrap.servers': self._servers})

    def write(self, event):
        self._producer.poll(0)

        # Asynchronously produce a message, the delivery report callback will
        # will be triggered (from poll or flush), when the message has
        # been successfully delivered or failed permanently.
        self._producer.produce(self._topic, event.to_bytes(), partition=self.partition_for_key(
            event.get_thread_id()), callback=KafkaWriter.delivery_report)

    def close(self):
        self._producer.flush()
        self._client.close()

    def partition_for_key(self, thread_id):
        return int(hashlib.sha512(thread_id).hexdigest(), 16) % self._partitions_count

    def _boot_topic(self):
        self._client = KafkaClient(self._servers)

        if not self._client.has_metadata_for_topic(self._topic):
            raise IOError('Kafka topic was not found.')

        self._partitions_count = len(
            self._client.get_partition_ids_for_topic(self._topic))

        if self._partitions_count == 0:
            raise IOError('Kafka topic does not have any partition.')

    @staticmethod
    def delivery_report(err, msg):
        if err is not None:
            logging.error('Event delivery failed: {}'.format(err))
        elif logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            logging.debug('Event delivered to {} [{}]'.format(
                msg.topic(), msg.partition()))

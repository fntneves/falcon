import logging
import hashlib
from confluent_kafka import Producer

class KafkaWriter:
    def __init__(self, partitions=1):
        self._producer = Producer({'bootstrap.servers': 'cloud83:9092'})
        self._partitions = partitions

    def append(self, event):
        self._producer.poll(0)

        # Asynchronously produce a message, the delivery report callback
        # will be triggered from poll() above, or flush() below, when the message has
        # been successfully delivered or failed permanently.
        self._producer.produce('events', event.to_bytes(), partition=self.partition_for_key(
            event.get_thread_id()), callback=KafkaWriter.delivery_report)

    def __del__(self):
        self._producer.flush()

    def partition_for_key(self, thread_id):
        return int(hashlib.sha512(thread_id).hexdigest(), 16) % self._partitions

    @staticmethod
    def delivery_report(err, msg):
        if err is not None:
            logging.error('Message delivery failed: {}'.format(err))
        else:
            logging.info('Message delivered to {} [{}]'.format(
                msg.topic(), msg.partition()))

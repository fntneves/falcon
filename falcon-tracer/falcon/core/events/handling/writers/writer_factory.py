import os
import logging
from file_writer import JsonWriter, BinaryWriter
from kafka_writer import KafkaWriter

class WriterFactory:
    @staticmethod
    def create(driver):
        if driver == 'json':
            logging.info('Creating JSON writer...')
            return JsonWriter(os.getenv('WRITER_OUTPUT_FILE'))

        if driver == 'binary':
            logging.info('Creating Binary writer...')
            return BinaryWriter(os.getenv('WRITER_OUTPUT_FILE'))

        if driver == 'kafka':
            logging.info('Creating Kafka writer...')
            return KafkaWriter(os.getenv('KAFKA_SERVERS'))

        raise ValueError('Invalid [{}] writer driver.'.format(driver))

    @staticmethod
    def createFromConfig():
        driver = os.getenv('WRITER_DRIVER')

        return WriterFactory.create(driver)

import os
from file_writer import JsonWriter, BinaryWriter
from kafka_writer import KafkaWriter

class WriterFactory:
    @staticmethod
    def create(driver):
        if driver == 'json':
            return JsonWriter(os.getenv('WRITER_OUTPUT_FILE'))

        if driver == 'binary':
            return BinaryWriter(os.getenv('WRITER_OUTPUT_FILE'))

        if driver == 'kafka':
            return KafkaWriter(os.getenv('KAFKA_SERVERS'), os.getenv('KAFKA_TOPIC'))

        return None

    @staticmethod
    def createFromConfig():
        driver = os.getenv('WRITER_DRIVER')

        return WriterFactory.create(driver)

class BaseHandler:
    def boot(self):
        pass

    def handle(self, cpu, data, size):
        raise NotImplementedError

    def shutdown(self):
        pass

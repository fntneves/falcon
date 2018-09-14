import os
from py2neo import Graph

class Neo4jGraph(object):
    def __init__(self, uri, user, password):
        self._driver = Graph(uri, user=user, password=password)

    def add_connection(self, event):
        print "adding connection"
        pass

    def update_connection(self, event):
        print "updating connection"
        pass

    def remove_connection(self, event):
        print "removing connection"
        pass

    def __del__(self):
        if (self._driver is not None)
            self._driver.close()


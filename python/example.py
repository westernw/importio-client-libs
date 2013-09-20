import logging, importio, threading, json

# Example code for using the client library

class _Latch(object):
    def __init__(self, count=1):
        self.count = count
        self.lock = threading.Condition()

    def countDown(self):
        with self.lock:
            self.count -= 1

            if self.count <= 0:
                self.lock.notifyAll()

    def await(self):
        with self.lock:
            while self.count > 0:
                self.lock.wait()

logging.basicConfig(level=logging.INFO)

proxies = { "http":"192.168.56.1:8888"}

# If using API Key
# client = ImportIO(host="http://query.qa2.import.io:8888", userId="d08d14f3-6c98-44af-a301-f8d4288ecce3", apiKey="tMFNJzaaLe8sgYF9hFNhKI7akyiPLMhfu8U2omNVCVr5hqWWLyiQMApDDyUucQKF++BAoVi6jnGnavYqRKP/9g==", proxies=proxies)

# If using username and password
client = importio.ImportIO(proxies=proxies)
client.login("xxx", "xxx")

client.connect()

# use a latch to stop the program from exiting
latch = _Latch(3)

def callback(query, message):
    
    if message["type"] == "MESSAGE": 
        print "Got data!"
        print json.dumps(message["data"],indent = 4)
        
    if query.finished(): latch.countDown()
    
client.query({"input":{"query":"mac mini"},"connectorGuids":["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback )
client.query({"input":{"query":"ubuntu"},"connectorGuids":["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback )
client.query({"input":{"query":"ibm"},"connectorGuids":["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback )

# wait until all 3 queryies are finished
latch.await()
'''
import.io client library - client classes

This file contains the main classes required to connect to and query import.io APIs

Dependencies: Python 2.7

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/python
'''

import threading, logging, uuid, json, urllib, urllib2, cookielib, gzip, Queue
from cookielib import CookieJar, DefaultCookiePolicy
from _pyio import BytesIO

# Set up the logging configuration if anyone wants useful output
logging.basicConfig()
logger = logging.getLogger(__name__)

class query_state:
    '''
    Represents a single query into the import.io platform
    '''

    def __init__(self, callback, query):
        '''
        Initialises the new query object with inputs and default state
        '''
        self.query = query
        self.jobsSpawned = 0
        self.jobsStarted = 0
        self.jobsCompleted = 0
        self._finished = False
        self._callback = callback
    
    def _onMessage(self, data):
        '''
        Method that is called when a new message is received
        '''

        # Check the type of the message to see what we are working with
        msgType = data["type"]
        if msgType == u"SPAWN":
            # A spawn message means that a new job is being initialised on the server
            self.jobsSpawned+=1
        elif msgType == u"INIT" or msgType == u"START":
            # Init and start indicate that a page of work has been started on the server
            self.jobsStarted+=1
        elif msgType == u"STOP":
            # Stop indicates that a job has finished on the server
            self.jobsCompleted+=1
        
        # Update the finished state
        # The query is finished if we have started some jobs, we have finished as many as we started, and we have started as many as we have spawned
        # There is a +1 on jobsSpawned because there is an initial spawn to cover initialising all of the jobs for the query
        self._finished = self.jobsStarted > 0 and self.jobsStarted is self.jobsCompleted and self.jobsSpawned + 1 is self.jobsStarted;
        
        # These error conditions mean the query has been terminated on the server
        # It either errored on the import.io end, the user was not logged in, or the query was cancelled on the server
        if msgType == u"ERROR" or msgType == u"UNAUTH" or msgType == u"CANCEL":
            self._finished = True;
        
        # Now we have processed the query state, we can return the data from the message back to listeners
        self._callback(self, data)
        
    def finished(self):
        '''
        Returns boolean - true if the query has been completed or terminated
        '''
        return self._finished

class importio:
    '''
    The main import.io client, used for managing the message channel and sending queries and receiving data
    '''
    
    def __init__(self, host="https://query.import.io", proxies={}, userId=None, apiKey=None):
        '''
        Initialises the client library with its configuration
        '''
        self.host = host
        self.proxies = proxies
        self.cookies = {}
        self.msgId = 1
        self.clientId = None
        self.url = "%s/query/comet/" % host
        self.messagingChannel = u"/messaging"
        self.queries = {}
        self.userId = userId
        self.apiKey = apiKey
        self.cj = cookielib.CookieJar()
        self.opener = urllib2.build_opener(urllib2.ProxyHandler(self.proxies), urllib2.HTTPCookieProcessor(self.cj))
        self.queue = Queue.Queue()
        self.isConnected = False
        self.disconnecting = False
        # These variables serve to identify this client and its version to the server
        self.clientName = "import.io Python client"
        self.clientVersion = "2.0.0"
    
    def login(self, username, password, host="https://api.import.io"):
        '''
        If you want to use cookie-based authentication, this method will log you in with a username and password to get a session
        '''
        r = self.opener.open("%s/auth/login" % host, urllib.urlencode( {'username': username, 'password': password} ) ) 

        if r.code is not 200:
            raise Exception("Could not log in, code %s" % r.code)
    
    def request(self, channel, path="", data={}, throw=True):
        '''
        Helper method that makes a generic request on the messaging channel
        '''
        # These are CometD configuration values that are common to all requests we need to send
        data["channel"] = channel
        data["connectionType"] = "long-polling"

        # We need to increment the message ID with each request that we send
        data["id"] = self.msgId
        self.msgId += 1
        
        # If we have a client ID, then we need to send that (will be provided on handshake)
        if self.clientId is not None:
            data["clientId"] = self.clientId
            
        # Build the URL that we are going to request
        url = "%s%s" % (self.url, path)
        
        # If the user has chosen API key authentication, we need to send the API key with each request
        if self.apiKey is not None:
            url = "%s?&%s" % (url, urllib.urlencode({ "_user" : self.userId, "_apikey" : self.apiKey }) )
        
        # Build the request object we are going to use to initialise the request
        request = urllib2.Request(url)
        request.add_data(json.dumps([data]))
        request.add_header("Content-Type", "application/json;charset=UTF-8")
        request.add_header('Accept-encoding', 'gzip')
        request.add_header('import-io-client', self.clientName)
        request.add_header('import-io-client-version', self.clientVersion)

        # Send the request itself
        try:
            response = self.opener.open(request)
        except urllib2.HTTPError:
            raise Exception("Exception raised connecting to import.io for url %s" % url)

        # If the server responds non-200 we have a serious issue (configuration wrong or server down)
        if response.code != 200 :
            raise Exception("Unable to connect to import.io, status %s for url %s" % (response.code, url))
        
        # If the data comes back as gzip, we need to manually decode it
        if response.info().get('Content-Encoding') == 'gzip':
            # Unfortunately we need to buffer it in memory to decode the gzip: http://bugs.python.org/issue914340
            response.json = json.load(gzip.GzipFile(fileobj=BytesIO(response.read())))
        else:
            response.json = json.load(response)
        
        # Iterate through each of the messages in the response content
        for msg in response.json:
            # If the message is not successful, i.e. an import.io server error has occurred, decide what action to take
            if "successful" in msg and msg["successful"] is not True:
                errorMessage = "Unsuccessful request: %s" % msg
                if not self.disconnecting and self.isConnected:
                    # If we get a 402 unknown client we need to reconnect
                    if msg["error"] == "402::Unknown client":
                        logger.warn("402 received, reconnecting")
                        self.disconnect()
                        self.connect()
                    if throw:
                        raise Exception(errorMessage)
                    else:
                        logger.warn(errorMessage)
                else:
                    continue
        
            # Ignore messages that come back on a CometD channel that we have not subscribed to
            if msg["channel"] != self.messagingChannel : continue
                
            # Now we have a valid message on the right channel, queue it up to be processed
            self.queue.put(msg["data"])
        
        # We have finished processing the response messages, return the response in case the client wants anything else from it
        return response
    
    def handshake(self):
        '''
        This method uses the request helper to make a CometD handshake request to register the client on the server
        '''
        # Make the handshake request
        handshake = self.request("/meta/handshake", path="handshake", data={
            "version": "1.0",
            "minimumVersion": "0.9",
            "supportedConnectionTypes": [ "long-polling" ],
            "advice": {
                "timeout": 60000,
                "interval": 0
            }
        })
        # Set the Client ID from the handshake's response
        self.clientId = handshake.json[0]["clientId"]

    def subscribe(self, channel):
        '''
        This method uses the request helper to issue a CometD subscription request for this client on the server
        '''
        return self.request("/meta/subscribe", data={
            "subscription": channel
        })
    
    def connect(self):
        '''
        Connect this client to the import.io server if not already connected
        '''
        # Don't connect again if we're already connected
        if self.isConnected:
            return;

        # Do the hanshake request to register the client on the server
        self.handshake()
        
        # Register this client with a subscription to our chosen message channel
        self.subscribe(self.messagingChannel)

        # Now we are subscribed, we can set the client as connected
        self.isConnected = True

        # Python's HTTP requests are synchronous - so that user apps can run while we are waiting for long connections
        # from the import.io server, we need to pass the long-polling connection off to a thread so it doesn't block
        # anything else
        pollThread = threading.Thread(target=self.poll, args=())
        pollThread.daemon = True
        pollThread.start()
        
        # Similarly with the polling, we need to handle queued messages in a separate thread too
        queueThread = threading.Thread(target=self.pollQueue, args=())
        queueThread.daemon = True
        queueThread.start()

    def disconnect(self):
        '''
        Call this method to ask the client library to disconnect from the import.io server
        It is best practice to disconnect when you are finished with querying, so as to clean
        up resources on both the client and server
        '''
        # Send a "disconnected" message to all of the current queries, and then remove them
        for key, query in self.queries.iteritems():
            query._onMessage({ "type": "DISCONNECT", "requestId": key })
        self.queries = {}
        # Set the flag to notify handlers that we are disconnecting, i.e. open connect calls will fail
        self.disconnecting = True
        # Set the connection status flag in the library to prevent any other requests going out
        self.isConnected = False
        # Make the disconnect request to the server
        self.request("/meta/disconnect", throw=True)
        # Now we are disconnected we need to remove the client ID
        self.clientId = None
        # We are done disconnecting so reset the flag
        self.disconnecting = False

    def pollQueue(self):
        '''
        This method is called in a new thread to poll the queue of messages returned from the server
        and process them
        '''
        # This while will mean the thread keeps going until the client library is disconnected
        while self.isConnected:
            try:
                # Attempt to process the last message on the queue
                self.processMessage(self.queue.get())
            except:
                logger.error("Error", exc_info=True)

    def poll(self):
        '''
        This method is called in a new thread to open long-polling HTTP connections to the import.io
        CometD server so that we can wait for any messages that the server needs to send to us
        '''
        # While loop means we keep making connections until manually disconnected
        while self.isConnected:
            # Use the request helper to make the connect call to the CometD endpoint
            self.request("/meta/connect", path="connect", throw=False)
        
    def processMessage(self, data):
        '''
        This method is called by the queue poller to handle messages that are received from the import.io
        CometD server
        '''
        try:
            # First we need to look up which query object the message corresponds to, based on its request ID
            reqId = data["requestId"]
            query = self.queries[reqId]

            # Call the message callback on the query object with the data
            query._onMessage(data)

            # Clean up the query map if the query itself is finished
            if query.finished() and reqId in self.queries: del self.queries[reqId]
        except:
            logger.error("Error", exc_info=True)
        
    def query(self, query, callback):
        '''
        This method takes an import.io Query object and issues it to the server, calling the callback
        whenever a relevant message is received
        '''
        # Set the request ID to a random GUID
        # This allows us to track which messages correspond to which query
        query["requestId"] = str(uuid.uuid4())
        # Construct a new query state tracker and store it in our map of currently running queries
        self.queries[query["requestId"]] = query_state(callback, query)
        # Issue the query to the server
        self.request("/service/query", data={ "data": query })

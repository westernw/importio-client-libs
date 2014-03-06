'''
import.io client library - test cases

This file contains tests that verify the functionality of the import.io python client

Dependencies: Python 2.7

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/python
'''

import importio, latch, sys, uuid

# Retrieve the credentials from the command line
host = "https://query." + sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
userguid = sys.argv[4]
apikey = sys.argv[5]

'''
Test 1

Test that specifying incorrect username and password raises an exception
'''
try:
	client = importio.importio(host=host)
	client.login(str(uuid.uuid4()), str(uuid.uuid4()))
	print "Test 1: Failed (did not throw exception)"
	sys.exit(1)
except Exception:
	print "Test 1: Success"

'''
Test 2

Test that providing an incorrect user GUID raises an exception
'''
try:
	client = importio.importio(host=host, userId=str(uuid.uuid4()), apiKey=apikey)
	client.connect()
	print "Test 2: Failed (did not throw exception)"
	sys.exit(2)
except Exception:
	print "Test 2: Success"

'''
Test 3

Test that providing an incorrect API key raises an exception
'''
try:
	client = importio.importio(host=host, userId=userguid, apiKey=str(uuid.uuid4()))
	client.connect()
	print "Test 3: Failed (did not throw exception)"
	sys.exit(3)
except Exception:
	print "Test 3: Success"

'''
Test 4

Test that querying a source that doesn't exist returns an error
'''
test4latch = latch.latch(1)
test4pass = False

def test4callback(query, message):
	global test4pass

	if message["type"] == "MESSAGE" and "errorType" in message["data"] and message["data"]["errorType"] == "ConnectorNotFoundException":
		test4pass = True;

	if query.finished(): test4latch.countdown()

client = importio.importio(host=host, userId=userguid, apiKey=apikey)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ str(uuid.uuid4()) ] }, test4callback)

test4latch.await()
client.disconnect()

if not test4pass:
	print "Test 4: Failed (did not return an error message)"
	sys.exit(4)
else:
	print "Test 4: Success"

'''
Test 5

Test that querying a source that returns an error is handled correctly
'''
test5latch = latch.latch(1)
test5pass = False

def test5callback(query, message):
	global test5pass

	if message["type"] == "MESSAGE" and "errorType" in message["data"] and message["data"]["errorType"] == "UnauthorizedException":
		test5pass = True;

	if query.finished(): test5latch.countdown()

client = importio.importio(host=host, userId=userguid, apiKey=apikey)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "eeba9430-bdf2-46c8-9dab-e1ca3c322339" ] }, test5callback)

test5latch.await()
client.disconnect()

if not test5pass:
	print "Test 5: Failed (did not return an error message)"
	sys.exit(5)
else:
	print "Test 5: Success"

# Set up the expected data for the next two tests
expectedData = [
	"Iron Man",
	"Captain America",
	"Hulk",
	"Thor",
	"Black Widow",
	"Hawkeye"
]

'''
Test 6

Tests querying a working source with username and password
'''
test6latch = latch.latch(1)
test6data = []
test6pass = True

def test6callback(query, message):
	global test6data

	if message["type"] == "MESSAGE":
		for result in message["data"]["results"]:
			test6data.append(result["name"])

	if query.finished(): test6latch.countdown()

client = importio.importio(host=host, userId=userguid, apiKey=apikey)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test6callback)

test6latch.await()
client.disconnect()

for index, value in enumerate(test6data):
	if value != expectedData[index]:
		test6pass = False
		print "Test 6: Index %i does not match (%s, %s)" % (index, value, expectedData[index])

if not test6pass:
	print "Test 6: Failed (returned data did not match)"
	sys.exit(6)
else:
	print "Test 6: Success"

'''
Test 7

Tests querying a working source with username and password
'''
test7latch = latch.latch(1)
test7data = []
test7pass = True

def test7callback(query, message):
	global test7data

	if message["type"] == "MESSAGE":
		for result in message["data"]["results"]:
			test7data.append(result["name"])

	if query.finished(): test7latch.countdown()

client = importio.importio(host=host)
client.login(username, password)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test7callback)

test7latch.await()
client.disconnect()

for index, value in enumerate(test7data):
	if value != expectedData[index]:
		test7pass = False
		print "Test 7: Index %i does not match (%s, %s)" % (index, value, expectedData[index])

if not test7pass:
	print "Test 7: Failed (returned data did not match)"
	sys.exit(7)
else:
	print "Test 7: Success"
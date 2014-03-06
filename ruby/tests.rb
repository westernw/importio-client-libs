#
# import.io client library - test cases
# 
# This file contains tests that verify the functionality of the import.io Ruby client
#
# Dependencies: Ruby 1.9
#
# @author: dev@import.io
# @source: https://github.com/import-io/importio-client-libs/tree/master/python
# 

require "./importio.rb"
require "securerandom"

# Retrieve the credentials from the command line
host = ARGV[0]
username = ARGV[1]
password = ARGV[2]
userguid = ARGV[3]
apikey = ARGV[4]

# Test 1
#
# Test that specifying incorrect username and password raises an exception
begin
  client = Importio::new(nil, nil, "https://query." + host)
  client.login(SecureRandom.uuid, SecureRandom.uuid, "https://api." + host)
  puts "Test 1: Failed (did not throw exception)"
  exit 1
rescue
  puts "Test 1: Success"
end

# Test 2
#
# Test that providing an incorrect user GUID raises an exception
begin
  client = Importio::new(SecureRandom.uuid, apikey, "https://query." + host)
  client.connect
  puts "Test 2: Failed (did not throw exception)"
  exit 2
rescue
  puts "Test 2: Success"
end

# Test 3
#
# Test that providing an incorrect API key raises an exception
begin
  client = Importio::new(userguid, SecureRandom.uuid, "https://query." + host)
  client.connect
  puts "Test 3: Failed (did not throw exception)"
  exit 3
rescue
  puts "Test 3: Success"
end

# Test 4
#
# Test that querying a source that doesn't exist returns an error
test4pass = false

test4callback = lambda do |query, message|
  if message["type"] == "MESSAGE" and message["data"].key?("errorType") and message["data"]["errorType"] == "ConnectorNotFoundException"
    test4pass = true
  end
end

client = Importio::new(userguid, apikey, "https://query." + host)
client.connect
client.query({"input"=>{"query"=>"server"},"connectorGuids"=>[SecureRandom.uuid]}, test4callback )
client.join
client.disconnect

if !test4pass
  puts "Test 4: Failed (did not return an error message)"
  exit 4
else
  puts "Test 4: Success"
end

# Test 5
#
# Test that querying a source that doesn't exist returns an error
test5pass = false

test5callback = lambda do |query, message|
  if message["type"] == "MESSAGE" and message["data"].key?("errorType") and message["data"]["errorType"] == "UnauthorizedException"
    test5pass = true
  end
end

client = Importio::new(userguid, apikey, "https://query." + host)
client.connect
client.query({"input"=>{"query"=>"server"},"connectorGuids"=>["eeba9430-bdf2-46c8-9dab-e1ca3c322339"]}, test5callback )
client.join
client.disconnect

if !test5pass
  puts "Test 5: Failed (did not return an error message)"
  exit 5
else
  puts "Test 5: Success"
end

# Set up the expected data for the next two tests
expected_data = [
  "Iron Man",
  "Captain America",
  "Hulk",
  "Thor",
  "Black Widow",
  "Hawkeye"
]

# Test 6
#
# Tests querying a working source with user GUID and API key
test6data = []
test6pass = true

test6callback = lambda do |query, message|
  if message["type"] == "MESSAGE"
    for result in message["data"]["results"]
      test6data << result["name"]
    end
  end
end

client = Importio::new(userguid, apikey, "https://query." + host)
client.connect
client.query({"input"=>{"query"=>"server"},"connectorGuids"=>["1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2"]}, test6callback )
client.join
client.disconnect

test6data.each_with_index { |value, index|
  if value != expected_data[index]
    test6pass = false
    puts "Test 6: Index #{index} does not match, expected #{value}"
  end
}

if !test6pass
  puts "Test 6: Failed (returned data did not match)"
  exit 6
else
  puts "Test 6: Success"
end

# Test 7
#
# Tests querying a working source with username and password
test7data = []
test7pass = true

test7callback = lambda do |query, message|
  if message["type"] == "MESSAGE"
    for result in message["data"]["results"]
      test7data << result["name"]
    end
  end
end

client = Importio::new(nil, nil, "https://query." + host)
client.login(username, password, "https://api." + host)
client.connect
client.query({"input"=>{"query"=>"server"},"connectorGuids"=>["1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2"]}, test7callback )
client.join
client.disconnect

test7data.each_with_index { |value, index|
  if value != expected_data[index]
    test7pass = false
    puts "Test 7: Index #{index} does not match, expected #{value}"
  end
}

if !test7pass
  puts "Test 7: Failed (returned data did not match)"
  exit 7
else
  puts "Test 7: Success"
end
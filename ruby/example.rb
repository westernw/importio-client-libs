require './importio.rb'
require 'json' 

client = ImportIO::new("d9273f3-6c98-44af-a301-f8d4288ebbe3","tMFNJzytLe8sgYF1869csM8843iPLMhfu8U2omNVCVr5hqWWLyiQMApDDyUucQKF++BAoVi6jnGnavYqRKP/9g==","http://query.qa2.import.io:8888")
#client.proxy("192.168.56.1",8888)
#client.login("xxxx", "xxxx", "http://api.import.io")
client.connect()

callback = lambda do |query, message|
  if message["type"] == "MESSAGE"
    json = JSON.pretty_generate(message["data"])
    puts json
  end
end

client.query({"input"=>{"query"=>"mac mini"},"connectorGuids"=>["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback )

client.join
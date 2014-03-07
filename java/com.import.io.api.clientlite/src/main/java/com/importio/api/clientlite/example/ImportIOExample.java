package com.importio.api.clientlite.example;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.importio.api.clientlite.ImportIO;
import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

public class ImportIOExample {
	public static void main(String[] args) throws IOException, InterruptedException {

	    ImportIO client = new ImportIO(UUID.fromString("6d05ddb1-f13d-43f5-a785-2e4314b79fe5"), "MvKWjOcd3LXGsibVVix4V1Jpqo1ATwMdHX9Fbat3LbW63siyotoPLuEOwt/Ia3W17nVWmAY795RwxSy59IUP4Q==");
	    client.connect();

	    final CountDownLatch latch = new CountDownLatch(2);

	    // Setup the callback
	    MessageCallback messageCallback = new MessageCallback() {
	      public void onMessage(Query query, QueryMessage message, Progress progress) {
	        if ( message.getType() == MessageType.MESSAGE ) {
	          System.err.println( message );
	        }
	        if ( progress.isFinished() ) {
	          latch.countDown();
	        }
	      }
	    };

	    // List of Connector GUIDs for each query
	    List<UUID> connectorGuids;

	    // Input object for each query
	    Map<String, Object> queryInput;

	    // Query object for each query
	    Query query;


	    // Query for tile Basic
	    connectorGuids = Arrays.asList(
	      UUID.fromString("454cb1d5-d55c-407c-9f51-bddb0a08e543")
	    );
	    queryInput = new HashMap<String,Object>();
	    queryInput.put("input", "input");

	    query = new Query();
	    query.setConnectorGuids(connectorGuids);
	    query.setInput(queryInput);

	    client.query(query, messageCallback);


	    // Query for tile Basic
	    connectorGuids = Arrays.asList(
	      UUID.fromString("454cb1d5-d55c-407c-9f51-bddb0a08e543")
	    );
	    queryInput = new HashMap<String,Object>();
	    queryInput.put("input", "output");

	    query = new Query();
	    query.setConnectorGuids(connectorGuids);
	    query.setInput(queryInput);

	    client.query(query, messageCallback);


	    // wait until the query is finished
	    latch.await();

	    client.shutdown();
	  }
	
}

package com.importio.api.clientlite;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import lombok.val;

import org.junit.Test;

import com.importio.api.clientlite.data.ImportIOExecutingQuery;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;

public class ImportIOTest {

	@Test
	public void test() throws IOException, InterruptedException {
		
		val client = new ImportIO(UUID.fromString("d22d14f3-6c98-44af-a301-f822288ebbe3"), "tMFNJzytLe8sgYF9hFNhKI7akyiPLMhfu8UfomNVCVr5fqWWLyiQMfpDDyfucQKF++BAoVi6jnGnavYqRKP/9g==");
		
		// If doing login rather than API key
		// val client = new ImportIO();
		// client.login("xxx", "xxx");

		client.connect();
		
		val latch = new CountDownLatch(3);
		
		MessageCallback messageCallback = new MessageCallback() {
			public void onMessage(ImportIOExecutingQuery query, QueryMessage message) {
				System.err.println( message );
				if ( query.isFinished() ) {
					latch.countDown();
				}
			}
		};
		
		client.query(makeQuery("mac mini"), messageCallback );
		client.query(makeQuery("ibm"), messageCallback );
		client.query(makeQuery("handbag"), messageCallback );

		// wait until all 3 queryies are finished
		latch.await();
	}

	private Query makeQuery(String query) {
		val q = new Query();
		q.setInput(Collections.<String,Object>singletonMap("query",query));
		q.setConnectorGuids(Collections.singletonList(UUID.fromString("39df3fe4-c716-478b-9b80-bdbee43bfbde")));
		return q;
	}

}

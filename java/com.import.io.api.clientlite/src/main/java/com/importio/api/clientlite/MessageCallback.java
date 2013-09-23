package com.importio.api.clientlite;

import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;


/**
 * interface for handling messages from the Query API such as progress messages and returned results
 * @author dev
 *
 */
public interface MessageCallback {
	
	/**
	 * this method is called when a message is received from a query
	 */
	void onMessage(Query query, QueryMessage message, Progress progress);
}

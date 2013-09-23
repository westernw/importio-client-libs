package com.importio.api.clientlite;

import com.importio.api.clientlite.data.ImportIOExecutingQuery;
import com.importio.api.clientlite.data.QueryMessage;


/**
 * interface for handling messages from the Query API such as progress messages and returned results
 * @author dev
 *
 */
public interface MessageCallback {
	
	/**
	 * this method is called when a message is received from a query
	 * @param message
	 */
	void onMessage(ImportIOExecutingQuery query, QueryMessage message);
}

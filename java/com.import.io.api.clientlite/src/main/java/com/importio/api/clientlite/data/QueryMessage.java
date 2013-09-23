package com.importio.api.clientlite.data;
/*
 * Copyright (c) 2011 All Right Reserved, Kusiri Limited
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Kusiri Limited and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Kusiri Limited
 * and its suppliers and may be covered by U.K. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Kusiri Limited.
 */


import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * 
 * @author dev
 *
 */
@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class QueryMessage {

	/**
	 * The type of message being returned by the server, this can be used to track the progress of jobs and to relay error messages to users
	 */
	public static enum MessageType {

		INIT,
		
		MESSAGE,

		STOP,

		ERROR,

		START,

		UNAUTH,

		SPAWN, 
		
		CANCEL
	}

	/**
	 * the type of message being received, {@see MessageType}
	 */
	MessageType type;
	
	/**
	 * the data being returned as an object, this can be converted to a string or to json.
	 */
	Object data;
	
	/**
	 * the server-assigned queryId
	 */
	UUID queryId;
	
	/**
	 * the server-assigned exectionId
	 */
	UUID executionId;
	
	/**
	 * the requestId of the message, this refers to the {@link Query#getRequestId()} field of the query
	 */
	String requestId;
	
	/**
	 * the number of remaining messages the server expects to send
	 */
	Integer messagesRemaining;
	
	/**
	 * the connector guid that this result has originated from
	 */
	UUID connectorGuid;
	
	/**
	 * the connector guid version that this result has originated from
	 */
	UUID connectorVersionGuid;
}

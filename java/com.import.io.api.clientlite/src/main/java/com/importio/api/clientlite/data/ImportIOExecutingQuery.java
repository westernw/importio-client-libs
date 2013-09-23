package com.importio.api.clientlite.data;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

/**
 * CometdQueryHandler tracks the progress of a specific Query API query. And routes messages from the cometd client.
 * @author dev
 *
 */
@Log
@FieldDefaults(level=AccessLevel.PRIVATE)
public class ImportIOExecutingQuery {

	/**
	 * number of jobs that have started on the server side
	 */
	int jobsStarted;
	
	/**
	 * the number of jobs that have completed on the server side
	 */
	int jobsCompleted;
	
	/**
	 * the number of jobs spawned on the server
	 */
	int jobsSpawned;
	
	/**
	 * number of messages
	 */
	AtomicInteger messages = new AtomicInteger();
	
	/**
	 * the queryId used to cancel queries @see {@link CancelRequest}
	 */
	public UUID queryId;
	
	/**
	 * Returns true if the statement is finished.
	 */
	@Getter
	boolean finished = false;
	
	/**
	 * defines the callback where messages associated with this query are routed @see {@link ImportIOConnection#executeQuery(Query, MessageCallback)}
	 */
	MessageCallback messageCallback;
	
	/**
	 * the query that this handler wraps
	 */
	@Getter Query query;

	ExecutorService executorService;
	
	/**
	 * creates a query handler to manage the messaging and progress tracking of a query
	 * @param query 
	 * @param messageCallback
	 */
	public ImportIOExecutingQuery(ExecutorService executorService, Query query, MessageCallback messageCallback) {
		this.executorService = executorService;
		this.query = query;
		this.messageCallback = messageCallback;
	}

	
	/**
	 * routes the messages recived from the server to the callback defined in {@link ImportIOConnection#executeQuery(Query, MessageCallback)
	 * allows {@link this#isFinished()} to return true if there has been an unrecoverable error
	 * @param executorService 
	 * @param message
	 */
	public void onMessage(final QueryMessage message) {
		
		log.log(Level.INFO, "Received {0} message", message.getType());
		
		final Progress progress = updateProgress(message);
		
		if(messageCallback != null) {
			executorService.submit(new Runnable() {
				public void run() {
					messageCallback.onMessage(query, message, progress);
				}
			});
		}
		
	}

	@Synchronized
	private Progress updateProgress(QueryMessage message) {
		switch (message.getType()) {
		case SPAWN:
			jobsSpawned++;
			break;
		case INIT:
		case START:
			jobsStarted++;
			break;

		case STOP:
			jobsCompleted++;
			break;
		case MESSAGE:
			messages.incrementAndGet();
			break;
		case CANCEL:
		case ERROR:
		case UNAUTH:
		default:
			break;
		}
		finished = jobsStarted == jobsCompleted && jobsSpawned + 1 == jobsStarted && jobsStarted > 0;
		
		//if there is an error or the user is not authorised correctly then allow isFinished to return true by setting jobs to -1
		if(message.getType() == MessageType.ERROR || message.getType() == MessageType.UNAUTH || message.getType() == MessageType.CANCEL) {
			finished = true;
		}
		
		return getProgress();
	}
	

	/**
	 * returns a Progress object containing the progress information of the query
	 */
	public Progress getProgress() {
		return new Progress(finished, jobsSpawned, jobsStarted,jobsCompleted, messages.get());
	}

}

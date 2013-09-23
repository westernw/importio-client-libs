package com.importio.api.clientlite.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Contains the progress of the query in terms of the number of server-side jobs started and completed
 * @author dev
 *
 */
@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class Progress {
	final int jobsSpawned;
	final int jobsStarted;
	final int jobsCompleted;
	final int messages;
}
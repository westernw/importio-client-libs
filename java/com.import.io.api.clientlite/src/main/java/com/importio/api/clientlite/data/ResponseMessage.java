package com.importio.api.clientlite.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class ResponseMessage {
	Boolean successful;
	int id;
	String clientId;
	String channel;
	QueryMessage data;
}

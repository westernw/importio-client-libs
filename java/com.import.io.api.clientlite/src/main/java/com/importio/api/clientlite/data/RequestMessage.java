package com.importio.api.clientlite.data;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Data
@Accessors(chain=true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class RequestMessage {
	String channel;
	String connectionType = "long-polling";
	String version;
	String minimumVersion;
	String subscription;
	List<String> supportedConnectionTypes;
	int id;
	String clientId;
	Query data;
}

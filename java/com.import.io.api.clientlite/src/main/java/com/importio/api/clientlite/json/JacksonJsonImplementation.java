package com.importio.api.clientlite.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import com.importio.api.clientlite.data.RequestMessage;
import com.importio.api.clientlite.data.ResponseMessage;

public class JacksonJsonImplementation implements JsonImplementation {
	final ObjectMapper objectMapper = new ObjectMapper(){{setSerializationInclusion(Inclusion.NON_NULL);}}.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public void writeRequest(OutputStream outputStream, RequestMessage data) throws IOException {
		objectMapper.writeValue(outputStream, data);
	}

	public List<ResponseMessage> readResponse(InputStream inputStream) throws IOException {
		return objectMapper.readValue(inputStream, new TypeReference<List<ResponseMessage>>() {});
	}
}

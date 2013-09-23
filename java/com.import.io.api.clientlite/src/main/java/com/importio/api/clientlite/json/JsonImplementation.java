package com.importio.api.clientlite.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.importio.api.clientlite.data.RequestMessage;
import com.importio.api.clientlite.data.ResponseMessage;

public interface JsonImplementation {

	void writeRequest(OutputStream outputStream, RequestMessage data) throws IOException;

	List<ResponseMessage> readResponse(InputStream inputStream) throws IOException;

}

package com.importio.api.clientlite;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

import com.importio.api.clientlite.data.ImportIOExecutingQuery;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.RequestMessage;
import com.importio.api.clientlite.data.ResponseMessage;
import com.importio.api.clientlite.json.JacksonJsonImplementation;
import com.importio.api.clientlite.json.JsonImplementation;

@FieldDefaults(level=AccessLevel.PRIVATE) 
@Log
public class ImportIO {

	static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

	static final String UTF_8 = "UTF-8";

	static final String MESSAGING_CHANNEL = "/messaging";
	static final String USER_AGENT = "import-io-client-lite";
	
	final CookieManager cookieManager = new CookieManager();
	@Setter JsonImplementation jsonImplementation;
	final ThreadLocal<CookieManager> cookieManagers = new ThreadLocal<CookieManager>();
	final ConcurrentHashMap<String, ImportIOExecutingQuery> queries = new ConcurrentHashMap<String, ImportIOExecutingQuery>();

	private Thread pollThread;
	
	@Setter ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	@Setter String apiHost = "https://api.import.io";
	@Setter String queryHost = "https://query.import.io";
	
	int msgId = 1;
	String clientId;
	String apiKey;
	UUID userId;
	boolean isConnected;
	
	public ImportIO() {
		this(null,null);
	}
	
	public ImportIO(UUID userId, String apiKey) {
		this.userId = userId;
		this.apiKey = apiKey;
		setupCookieHandler();
	}

	private void setupCookieHandler() {
		val def = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieHandler() {
			
			@Override
			public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
				CookieManager cookieManager = cookieManagers.get();
				if ( cookieManager != null ) {
					cookieManager.put(uri, responseHeaders);
				} else {
					def.put(uri, responseHeaders);
				}
			}
			
			@Override
			public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
				CookieManager cookieManager = cookieManagers.get();
				if ( cookieManager != null ) {
					return cookieManager.get(uri, requestHeaders);
				}
				return def.get(uri, requestHeaders);
			}
		});
	}
	
	public void login(String username, String password) throws IOException {

		cookieManagers.set(cookieManager);
		try {
			val urlConnection = (HttpURLConnection) new URL( apiHost + "/auth/login" ).openConnection();
			urlConnection.setDoOutput(true); 
			urlConnection.setDoInput(true); 
			urlConnection.setRequestMethod("POST");
			
			urlConnection.setRequestProperty("Content-Type", FORM_CONTENT_TYPE);
			urlConnection.setRequestProperty("User-Agent", USER_AGENT);
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");
			
			val writer = new OutputStreamWriter(urlConnection.getOutputStream());
			writer.write("username=");
			writer.write(URLEncoder.encode(username,UTF_8));
			writer.write("&password=");
			writer.write(URLEncoder.encode(password,UTF_8));
			writer.close();
			
			if ( urlConnection.getResponseCode() != 200 ) {
				throw new IOException("Connect failed, status " + urlConnection.getResponseCode() );
			}
			
		} finally {
			cookieManagers.remove();
		}
		
	}
	
	private List<ResponseMessage> request(String channel, String path, RequestMessage data, boolean throwExceptionOnFail) throws IOException {
		
		cookieManagers.set(cookieManager);
		try {
		
			if ( data == null ) {
				data = new RequestMessage();
			}
			
			data.setChannel(channel);
			data.setId(msgId++);
			data.setClientId(clientId); 
			
			String url = format("%s/query/comet/%s", queryHost, path);
			if ( apiKey != null ) {
				StringBuilder builder = new StringBuilder(url);
				builder.append('?');
				builder.append("_user=");
				builder.append(userId.toString());
				builder.append('&');
				builder.append("_apikey=");
				builder.append(URLEncoder.encode(apiKey, UTF_8));
				url = builder.toString();
			}
			
			val urlConnection = (HttpURLConnection) new URL(url).openConnection();
			urlConnection.setRequestMethod("POST");
			
			urlConnection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
			urlConnection.setRequestProperty("User-Agent", USER_AGENT);
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");

			List<RequestMessage> requestArray = new ArrayList<RequestMessage>();
			requestArray.add(data);
			
			urlConnection.setDoOutput(true); 
			jsonImplementation.writeRequest(urlConnection.getOutputStream(), requestArray);
			
			if ( urlConnection.getResponseCode() != 200 ) {
				throw new IOException("Connect failed, status " + urlConnection.getResponseCode() );
			}
			
			val encoding = urlConnection.getContentEncoding();
			
			final List<ResponseMessage> list = jsonImplementation.readResponse(makeInputStream(urlConnection, encoding));
			for (val msg : list ) {
				if ( msg.getSuccessful() != null && ! msg.getSuccessful() ) {
					val err = "Unsuccessful request:" + msg;
					if ( throwExceptionOnFail ) {
						throw new IOException(err);
					}
					
					log.warning(err);
					continue;
				}
				
				if ( ! MESSAGING_CHANNEL.equals(msg.getChannel())) {
					continue;
				}

				try {
					processMessage(msg);
				} catch ( Exception e ) {
					log.log(Level.SEVERE, "Processing message failed: "+msg, e);
				}
			}
			
			return list;
			
		} finally {
			cookieManagers.remove();
		}
	}

	private void processMessage(final ResponseMessage msg) {		
		final ImportIOExecutingQuery query = queries.get(msg.getData().getRequestId());
		query.onMessage(msg.getData());
		if ( query.isFinished() ) {
			queries.remove(msg.getData().getRequestId());
		}
	}

	private InputStream makeInputStream(HttpURLConnection urlConnection, String encoding) throws IOException {
		InputStream resultingInputStream;

		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			resultingInputStream = new GZIPInputStream(urlConnection.getInputStream());
		} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			resultingInputStream = new InflaterInputStream(urlConnection.getInputStream(), new Inflater(true));
		} else {
			resultingInputStream = urlConnection.getInputStream();
		}
		return resultingInputStream;
	}

	public void connect() throws IOException {
		
		if(isConnected) {
			return;
		}

		isConnected = true;
		// default to jackson
		if ( jsonImplementation == null ) {
			jsonImplementation = new JacksonJsonImplementation();
		}
		
		handshake();
		request("/meta/subscribe", "subscribe", new RequestMessage().setSubscription(MESSAGING_CHANNEL), true);
		pollThread = new Thread(new Runnable() {
			public void run() {
				poll();
			}
		});
		pollThread.setDaemon(true);
		pollThread.start();

	}

	private void poll() {
		while (isConnected) {
			try {
				request("/meta/connect", "connect", null, false);
			} catch (IOException e) {
				log.log(Level.WARNING, "Exception thrown", e);
			}
		}
	}

	private void handshake() throws IOException {
		val list = request("/meta/handshake", "handshake", new RequestMessage().setMinimumVersion("0.9").setVersion("1.0").setSupportedConnectionTypes(Arrays.asList("long-polling")), true);
		clientId = list.get(0).getClientId();
	}
	
	public void query(Query query, MessageCallback callback) throws IOException {
		query.setRequestId(UUID.randomUUID().toString());
		queries.put(query.getRequestId(), new ImportIOExecutingQuery(executorService, query, callback));
		request("/service/query", "", new RequestMessage().setData(query), true);
	}

	public void shutdown() throws IOException {
		request("/meta/disconnect", "", null, true);
		executorService.shutdown();
		isConnected = false;
	}
	
}

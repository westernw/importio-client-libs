package com.importio.api.clientlite.data;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * Contains all the parameters of a query, for example the mixGuid, mixKey and query input.
 * @author dev
 *
 */
@Data
@Accessors(chain=true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class Query {
	
	public static enum Format { JSON, HTML, XML }
	
	/**
	 * object guid
	 */
	UUID guid;
	
	/**
	 * The identifier of the query
	 */
	String requestId;
	
	/**
	 * The guid of the mix that the query is aimed at
	 */
	List<UUID> connectorGuids;
	
	/**
	 * the query input map. N.B this can have multiple fields therefore is a map
	 */
	Map<String, Object> input;
	
	Integer maxPages;
	
	Integer startPage;
	
	/**
	 * Whether or not to unpack values into hierarchical objects
	 */
	boolean asObjects;
	
	/**
	 * currently unused
	 */
	boolean returningSource;
	
	Format format = Format.JSON;

}

package com.livily.db;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.cache.CachingDatastoreServiceFactory;
import com.visitors.track.EventTrack;

public class DBUtil
{

	/** Datastore Key id */
	public final static String DATASTORE_KEY_IN_JSON = "id";
	public final static String DATASTORE_KEY_IN_DATASTORE = "__key__";

	// Get Single Record based on Search Criteria
	/**
	 * Returns Single Record based on Search Criteria
	 * 
	 * @param entityName
	 *            Name of the Entity
	 * @param searchCriteria
	 *            JSONObject
	 * @return JSONObject
	 */
	public static JSONObject getSingleRecord(String entityName, JSONObject searchCriteria)
	{

		FetchOptions fOption = FetchOptions.Builder.withLimit(1);
		try
		{
			Entity entity = getPreparedQuery(entityName, searchCriteria).asList(fOption).get(0);
			return EntityUtil.getJSONObjectFromEntity(entity);
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;

		}

		// Entity entity = getPreparedQuery(entityName,
		// searchCriteria).asSingleEntity();

		// return EntityUtil.getJSONObjectFromEntity(entity);

	}

	// Find Record based on AutoID
	/**
	 * Finds Record based on AutoID
	 * 
	 * @param entityName
	 *            Name of the Entity
	 * @param id
	 *            Key id
	 * @return
	 */
	public static JSONObject getSingleRecordFromID(String entityName, String id)
	{

		Entity entity = EntityUtil.getEntityFromId(entityName, id);
		if (entity == null)
			return null;

		return EntityUtil.getJSONObjectFromEntity(entity);
	}

	// Execute Query Directly
	/**
	 * Executes Query Directly
	 * 
	 * @param query
	 * @return JSONArray with results JSON
	 */
	public static String getQueryKey(String entityName, JSONObject searchItems)
	{

		// Query
		Query query = getQuery(entityName, searchItems);

		// Get keys
		query.setKeysOnly();

		PreparedQuery preparedQuery = getPreparedQuery(entityName, searchItems);
		for (Entity result : preparedQuery.asIterable())
			return KeyFactory.keyToString(result.getKey());

		return null;

	}

	// Execute Query Directly
	/**
	 * Executes Query Directly
	 * 
	 * @param query
	 * @return JSONArray with results JSON
	 */
	public static int executeQueryCount(Query query, int limit)
	{

		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();

		// PreparedQuery contains the methods for fetching query results
		// from the datastore
		PreparedQuery preparedQuery = getPreparedQuery(query);
		if (limit == -1)
			return preparedQuery.countEntities();

		System.out.println("limit = " + limit);

		try
		{
			return preparedQuery.countEntities(FetchOptions.Builder.withLimit(limit));
		}
		catch (Exception e)
		{
			System.out.println("Exception in DBUtil executeQueryCount = " + e.getMessage());

			// Get keys
			query.setKeysOnly();

			preparedQuery = datastore.prepare(query);

			int count = 0;
			for (Entity result : preparedQuery.asIterable())
				count++;

			return (count > limit) ? limit : count;
		}

	}

	// Execute Query Directly
	/**
	 * Executes Query Directly
	 * 
	 * @param query
	 * @return JSONArray with results JSON
	 */
	public static int executeQueryCount(String entityName, JSONObject searchItems, int limit)
	{

		// Query
		Query query = getQuery(entityName, searchItems);

		return executeQueryCount(query, limit);

	}

	// Execute Query Directly
	/**
	 * Executes Query Directly
	 * 
	 * @param query
	 * @return JSONArray with results JSON
	 */
	public static JSONArray executeQuery(Query query)
	{
		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();

		// PreparedQuery contains the methods for fetching query results
		// from the datastore
		PreparedQuery pq = datastore.prepare(query);

		// Get Results
		JSONArray jsonArray = new JSONArray();
		for (Entity result : pq.asIterable())
		{

			JSONObject resultJSONObject = EntityUtil.getJSONObjectFromEntity(result);
			jsonArray.put(resultJSONObject);
		}

		return jsonArray;

	}

	/**
	 * Get ID from JSONObject
	 * 
	 * @param json
	 * @return
	 */
	public static String getId(JSONObject json)
	{

		try
		{
			return json.getString(DATASTORE_KEY_IN_JSON);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	// Insert new record based on JSONObject - DOES NOT CHECK FOR DUPLICATES
	/**
	 * Insert new record based on JSONObject - DOES NOT CHECK FOR DUPLICATES
	 * 
	 * @param entityName
	 *            Entity name
	 * @param newRecord
	 *            JSONObject with new record details
	 * @return JSONObject
	 */
	public static JSONObject add(String entityName, JSONObject newRecord)
	{

		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();

		// Get Entity
		Entity entity = EntityUtil.getEntityFromJSONObject(entityName, newRecord);
		datastore.put(entity);

		// Get new JSONObject
		return EntityUtil.getJSONObjectFromEntity(entity);
	}

	// Updates only the fieds in the JSON - the rest are intact
	/**
	 * Updates only the fieds in the JSON - the rest are intact
	 * 
	 * @param entityName
	 *            Entity name
	 * @param id
	 *            Key id
	 * @param updatesJSONObject
	 *            Updated JSONObect
	 * @return JSONObject
	 */
	public static JSONObject updatePartial(String entityName, String id, JSONObject updatesJSONObject)
	{

		// Get Entity
		Entity entity = EntityUtil.getEntityFromId(entityName, id);
		if (entity == null)
			return null;

		try
		{
			// Add this JSON as text object
			if (entity.hasProperty(EventTrack.VISITOR_PATH))
			{
				JSONArray array = new JSONArray(((Text) entity.getProperty(EventTrack.VISITOR_PATH)).getValue());
				JSONArray logarray = new JSONArray(((Text) updatesJSONObject.get(EventTrack.VISITOR_PATH)).getValue());

				array.put(logarray.getJSONObject(0));

				updatesJSONObject.put(EventTrack.VISITOR_PATH, new Text(array.toString()));

			}
		}
		catch (Exception e)
		{
		}

		// Update Entity
		// Iterate through JSONObject
		Iterator<String> itr = updatesJSONObject.keys();
		while (itr.hasNext())
		{

			// Get Property Name
			String propertyName = itr.next();

			try
			{

				entity.setProperty(propertyName, updatesJSONObject.get(propertyName));
			}
			catch (Exception e)
			{

				e.printStackTrace();
			}
		}

		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();

		datastore.put(entity);
		return EntityUtil.getJSONObjectFromEntity(entity);
	}

	// Get Prepared Query from the search criteria
	/**
	 * Get Prepared Query from the search criteria
	 * 
	 * @param entityName
	 *            Entity name
	 * @param searchCriteria
	 *            JSONObject to search
	 * @return PreparedQuery
	 */
	private static PreparedQuery getPreparedQuery(String entityName, JSONObject searchCriteria)

	{

		// Query
		Query query = getQuery(entityName, searchCriteria);

		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(query);

		return pq;

	}

	/**
	 * Get Prepared Query from the search criteria
	 * 
	 * @param entityName
	 *            Entity name
	 * @param searchCriteria
	 *            JSONObject to search
	 * @return PreparedQuery
	 */
	private static PreparedQuery getPreparedQuery(Query query)

	{

		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(query);

		return pq;

	}

	private static Query getQuery(String entityName, JSONObject searchCriteria)

	{

		// Query
		Query query = new Query(entityName);

		// Iterate through JSONObject and Add filters
		Iterator<String> itr = searchCriteria.keys();

		while (itr.hasNext())

		{
			// Get Property Name
			String propertyName = itr.next();

			// Set property - do not store key

			if (!propertyName.equalsIgnoreCase(DATASTORE_KEY_IN_JSON))

				try
				{
					query.addFilter(propertyName, Query.FilterOperator.EQUAL, searchCriteria.get(propertyName));

				}
				catch (Exception e)
				{

				}

		}

		System.out.println("query = " + query);

		return query;

	}

	/**
	 * Get Multiple Records
	 * 
	 * @param entityName
	 *            Entity name
	 * @param searchCriteria
	 *            Search JSON
	 * @return JSONArray with result JSONObjects
	 */
	public static JSONArray getRecords(String entityName, JSONObject searchCriteria)

	{

		// Get PreparedQuery from Search Criteria
		PreparedQuery pq = getPreparedQuery(entityName, searchCriteria);

		// Get Results
		JSONArray jsonArray = new JSONArray();

		for (Entity result : pq.asIterable())
		{
			JSONObject resultJSONObject = EntityUtil.getJSONObjectFromEntity(result);

			jsonArray.put(resultJSONObject);

		}

		return jsonArray;

	}

	/**
	 * Finds Record based on cursor and page size
	 * 
	 * @param query
	 * @param cursor
	 *            id for fetching next records
	 * @param pageSize
	 *            number of records need to be fetched
	 * 
	 * @return JSONObject
	 */
	public static JSONArray executeQueryWithLimit(Query query, int pageSize) throws Exception
	{

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(pageSize);

		// PreparedQuery contains the methods for fetching query results
		// from the datastore
		PreparedQuery pq = getPreparedQuery(query);

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);

		// Get Results
		JSONArray jsonArray = new JSONArray();
		for (Entity result : results)
		{
			JSONObject resultJSONObject = EntityUtil.getJSONObjectFromEntity(result);
			jsonArray.put(resultJSONObject);

		}

		return jsonArray;
	}
	
	
	/**
	 * Finds Record based on cursor and page size
	 * 
	 * @param query
	 * @param cursor
	 *            id for fetching next records
	 * @param pageSize
	 *            number of records need to be fetched
	 * 
	 * @return JSONObject
	 */
	public static JSONArray executeQueryWithLimitAndOffset(Query query, int offset, int pageSize) throws Exception
	{

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(pageSize);
		fetchOptions = fetchOptions.offset(offset);

		// PreparedQuery contains the methods for fetching query results
		// from the datastore
		PreparedQuery pq = getPreparedQuery(query);

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);

		// Get Results
		JSONArray jsonArray = new JSONArray();
		for (Entity result : results)
		{
			JSONObject resultJSONObject = EntityUtil.getJSONObjectFromEntity(result);
			jsonArray.put(resultJSONObject);

		}

		return jsonArray;
	}

	/**
	 * Finds Record based on cursor and page size
	 * 
	 * @param query
	 * @param cursor
	 *            id for fetching next records
	 * @param pageSize
	 *            number of records need to be fetched
	 * 
	 * @return JSONObject
	 */
	public static JSONArray executeQueryUsingCursor(String entityName, JSONObject searchCriteria, String cursor,
			String pageSize) throws Exception
	{

		int pageCount = Integer.parseInt(pageSize);
		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(pageCount);

		System.out.println("cursor " + cursor);

		if (StringUtils.isNotBlank(cursor))
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursor));

		// PreparedQuery contains the methods for fetching query results
		// from the datastore
		PreparedQuery pq = getPreparedQuery(entityName, searchCriteria);

		// Get total count
		int count = executeQueryCount(entityName, searchCriteria, 10000);

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);

		int max = results.size(), index = 0;

		System.out.println("max = " + max);

		// Get Results
		JSONArray jsonArray = new JSONArray();
		for (Entity result : results)
		{
			JSONObject resultJSONObject = EntityUtil.getJSONObjectFromEntity(result);
			if (++index == max && index == pageCount)
			{
				resultJSONObject.put("cursor", results.getCursor().toWebSafeString());
				resultJSONObject.put("total_count", count);

			}

			jsonArray.put(resultJSONObject);

		}

		return jsonArray;
	}

}
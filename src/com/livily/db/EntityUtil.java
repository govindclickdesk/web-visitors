package com.livily.db;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.cache.CachingDatastoreServiceFactory;

public class EntityUtil
{

	/** Datastore Key id */
	public final static String DATASTORE_KEY_IN_JSON = "id";

	/**
	 * Returns Entity from the Key id
	 * 
	 * @param entityName
	 *            Name of the Entity
	 * @param id
	 *            Key id
	 * @return
	 */
	public static Entity getEntityFromId(String entityName, String id)
	{
		// Get Datastore
		DatastoreService datastore = CachingDatastoreServiceFactory.getDatastoreService();

		// Get Entity
		try
		{
			Key key = KeyFactory.stringToKey(id);
			return datastore.get(key);
		}
		catch (Exception e)
		{
			return null;
		}

	}

	// Creates JSONObject from Entity - stores as one element in JSONObject
	/**
	 * Creates JSONObject from Entity - stores as one element in JSONObject
	 * 
	 * @param entity
	 *            Entity Object
	 * @return JSONObject
	 */
	public static JSONObject getJSONObjectFromEntity(Entity entity)
	{

		// Create new JSONObject
		JSONObject jsonObject = new JSONObject();

		// Get Key
		String keyStr = KeyFactory.keyToString(entity.getKey());

		// Store Key
		try
		{
			jsonObject.put(DATASTORE_KEY_IN_JSON, keyStr);
			entity.removeProperty(DATASTORE_KEY_IN_JSON);
		}
		catch (Exception e1)
		{
		}

		// Get properties
		Map properties = entity.getProperties();

		// Iterate through properties
		Set set = properties.keySet();
		Iterator<String> itr = set.iterator();
		while (itr.hasNext())
		{

			try
			{
				// Get Property Name
				String propertyName = itr.next();

				// Continue if properName is password field
				if (StringUtils.isNotBlank(propertyName) && propertyName.toLowerCase().indexOf("password") != -1)
					continue;

				// Get value
				Object value = properties.get(propertyName);

				jsonObject.put(propertyName, value);

			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return jsonObject;
	}

	// Creates Entity from JSONObject - appropriate key is set if present
	/**
	 * Creates Entity from JSONObject - appropriate key is set if present
	 * 
	 * @param entityName
	 *            Entity name
	 * @param jsonObject
	 *            JSONOject to create entity to add to DB
	 * @return Entity Object
	 */
	public static Entity getEntityFromJSONObject(String entityName, JSONObject jsonObject)
	{

		// Get Key
		Key key = null;
		try
		{
			String keyStr = jsonObject.getString(DATASTORE_KEY_IN_JSON);
			if (keyStr != null)
				key = KeyFactory.stringToKey(keyStr);
			// System.out.println("Key " + key);
		}
		catch (Exception e)
		{
		}

		// Create entity with proper key
		Entity entity;
		if (key == null)
			entity = new Entity(entityName);
		else
			entity = new Entity(entityName, key);

		// Iterate through JSONObject
		Iterator<String> itr = jsonObject.keys();
		while (itr.hasNext())
		{

			// Get Property Name
			String propertyName = itr.next();

			// Set property - do not store key
			if (!propertyName.equalsIgnoreCase(DATASTORE_KEY_IN_JSON))
				try
				{
					entity.setProperty(propertyName, jsonObject.get(propertyName));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
		}

		return entity;
	}

}
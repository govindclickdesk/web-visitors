package com.visitors.track;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.livily.db.DBUtil;

public class EventTrack
{

	// Db Name
	public static final String DB_NAME = "event_log";

	// Properties
	public static final String EVENT_NAME = "event";

	public static enum EVENT
	{
		WEB_VISITOR, AGENT, VISITOR_UPDATE, GET_VISITOR_INFO

	}

	public static final String UUID = "uuid";
	public static final String VISITOR_INFO = "visitor_info";
	public static final String CHANNEL_NAME = "channel_name";
	public static final String DATE = "date";
	public static final String COUNTRY = "country";
	public static final String OS = "os";
	public static final String DEPARTMENT_ID = "widget_id";
	public static final String VISITOR_STATUS = "status";

	public static final String VISITOR_VISIT_TYPE = "type";

	public static final String VISITOR_PATH = "visitor_path";

	public static enum VISIT_TYPE
	{
		NEW, RETURN
	};

	public static enum STATUS
	{
		ACTIVE, INACTIVE
	};

	/**
	 * Add Log to event log DB
	 * 
	 * @param logJSON
	 * @param properties
	 * @return
	 */
	public static JSONObject trackLog(String channelName, JSONObject logJSON) throws Exception
	{

		String keyId = getLogDBID(new JSONObject().put(UUID, channelName));
		if (keyId == null)
			return DBUtil.add(DB_NAME, logJSON);

		else
			return updateLog(keyId, logJSON);

	}

	public static JSONObject updateLog(String keyId, JSONObject updates)
	{
		return DBUtil.updatePartial(DB_NAME, keyId, updates);
	}

	public static JSONObject getLog(JSONObject searchItems)
	{
		return DBUtil.getSingleRecord(DB_NAME, searchItems);
	}

	public static JSONArray getLogs(JSONObject searchItems)
	{
		return DBUtil.getRecords(DB_NAME, searchItems);
	}

	public static int getLogCount(JSONObject searchItems)
	{
		return DBUtil.executeQueryCount(DB_NAME, searchItems, -1);
	}

	public static String getLogDBID(JSONObject searchItems)
	{
		return DBUtil.getQueryKey(DB_NAME, searchItems);
	}

	public static JSONArray getActiveChannelUsers(String channelName, String cursorString, String pageSize,
			String widgetIds) throws Exception
	{
		JSONObject searchCriteria = new JSONObject().put(CHANNEL_NAME, channelName).put(VISITOR_STATUS,
				STATUS.ACTIVE.toString());

		// Implement cursor request
		if (StringUtils.isNotBlank(pageSize))
			return DBUtil.executeQueryUsingCursor(DB_NAME, searchCriteria, cursorString, pageSize);

		if (StringUtils.isBlank(widgetIds))
			return DBUtil.getRecords(DB_NAME, searchCriteria);

		Query query = new Query(DB_NAME);
		query.addFilter(CHANNEL_NAME, FilterOperator.EQUAL, channelName);
		query.addFilter(VISITOR_STATUS, FilterOperator.EQUAL, STATUS.ACTIVE.toString());
		query.addFilter("widget_id", FilterOperator.IN, new HashSet(Arrays.asList(widgetIds.split(","))));

		return DBUtil.executeQuery(query);

	}

	public static int getActiveChannelUsersCount(String channelName, String widgetIds) throws Exception
	{

		Query query = new Query(DB_NAME);
		query.addFilter(CHANNEL_NAME, FilterOperator.EQUAL, channelName);
		query.addFilter(VISITOR_STATUS, FilterOperator.EQUAL, STATUS.ACTIVE.toString());
		if (StringUtils.isNotBlank(widgetIds))
			query.addFilter("widget_id", FilterOperator.IN, new HashSet(Arrays.asList(widgetIds.split(","))));

		return DBUtil.executeQueryCount(query, 20000);

	}

}
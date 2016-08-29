package com.visitors.track;

import java.util.Arrays;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.visitors.track.EventTrack.STATUS;
import com.visitors.util.HTTPUtil;
import com.visitors.util.JSONUtil;

public class EventTrackUtil
{

	public static final String[] whiteListChannels = new String[] { "21NO4c", "1Feu5", "16h4", "YLC" };
	public static final String[] whiteListChannelURLPost = new String[] { "qa", "pprod" };

	public static final String POST_URL = "http://my.clickdesk.com/pusher-webhook";

	public static JSONObject createWebVisitorInsertQueryJSON(JSONObject updatedNewJSON, HttpServletRequest req,
			String delim) throws Exception
	{

		String DEVIDENT_PARAM = delim;
		String channelName = req.getParameter("channel_name");

		JSONObject logJSON = new JSONObject();
		logJSON.put(DEVIDENT_PARAM + EventTrack.EVENT_NAME + DEVIDENT_PARAM, req.getParameter("command"));
		logJSON.put(DEVIDENT_PARAM + EventTrack.UUID + DEVIDENT_PARAM, channelName);
		logJSON.put(DEVIDENT_PARAM + EventTrack.VISITOR_INFO + DEVIDENT_PARAM, updatedNewJSON.toString());
		logJSON.put(DEVIDENT_PARAM + EventTrack.CHANNEL_NAME + DEVIDENT_PARAM,
				channelName.substring(0, channelName.lastIndexOf("-")));

		JSONObject infoJSON = updatedNewJSON.getJSONObject("info");
		logJSON.put(DEVIDENT_PARAM + EventTrack.DEPARTMENT_ID + DEVIDENT_PARAM, infoJSON.getString("widget_id"));

		JSONObject userInfo = new JSONObject(infoJSON.getString("visitor_info"));
		String[] fields = new String[] { "country_code", "os", "browser", "referrer", "url", "page_title",
				"created_time" };
		for (String string : fields)
		{
			String val = JSONUtil.getJSONValue(userInfo, string);
			if (StringUtils.isBlank(val))
				val = "";

			logJSON.put(DEVIDENT_PARAM + string + DEVIDENT_PARAM, val);
		}

		logJSON.put(DEVIDENT_PARAM + EventTrack.VISITOR_STATUS + DEVIDENT_PARAM, STATUS.ACTIVE.toString());
		logJSON.put(DEVIDENT_PARAM + EventTrack.DATE + DEVIDENT_PARAM, Calendar.getInstance().getTimeInMillis());

		return logJSON;
	}

	public static JSONObject createWebVisitorLog(JSONObject updatedNewJSON, HttpServletRequest req) throws Exception
	{

		String channelName = req.getParameter("channel_name");

		JSONObject logJSON = new JSONObject();
		logJSON.put(EventTrack.EVENT_NAME, req.getParameter("command"));
		logJSON.put(EventTrack.UUID, channelName);
		logJSON.put(EventTrack.VISITOR_INFO, updatedNewJSON.toString());
		logJSON.put(EventTrack.CHANNEL_NAME, channelName.substring(0, channelName.lastIndexOf("-")));

		JSONObject infoJSON = updatedNewJSON.getJSONObject("info");
		logJSON.put(EventTrack.DEPARTMENT_ID, infoJSON.getString("widget_id"));

		JSONObject userInfo = new JSONObject(infoJSON.getString("visitor_info"));
		String[] fields = new String[] { "country_code", "os", "browser", "referrer", "url", "page_title",
				"created_time" };
		for (String string : fields)
		{
			String val = JSONUtil.getJSONValue(userInfo, string);
			if (StringUtils.isBlank(val))
				continue;

			logJSON.put(string, val);
		}

		logJSON.put(EventTrack.VISITOR_STATUS, STATUS.ACTIVE.toString());
		logJSON.put(EventTrack.DATE, Calendar.getInstance().getTimeInMillis());

		try
		{
			JSONObject visitorPathJSON = new JSONObject();
			String[] path_fields = new String[] { "url", "page_title", EventTrack.DATE };
			for (String string : path_fields)
			{
				visitorPathJSON.put(string, JSONUtil.getJSONValue(logJSON, string));
			}

			// Add this JSON as text object
			logJSON.put(EventTrack.VISITOR_PATH, new JSONArray().put(visitorPathJSON).toString());
		}
		catch (Exception e)
		{
		}

		return logJSON;
	}

	public static void sendEventLogToClickDesk(JSONObject messageJSON, String whiteListChannelName) throws Exception
	{
		try
		{
			if (whiteListChannelName.contains("-"))
			{
				whiteListChannelName = whiteListChannelName.split("-")[0];
				messageJSON.put("id", whiteListChannelName.split("-")[1]);
			}
		}
		catch (Exception e)
		{
		}

		HTTPUtil.accessURLUsingPost(POST_URL, messageJSON.toString());

		try
		{

			System.out.println("whiteListChannelName = " + whiteListChannelName);

			if (Arrays.asList(whiteListChannels).contains(whiteListChannelName))
			{
				for (String version : whiteListChannelURLPost)
				{
					String postURL = POST_URL.replace("my.", version + ".");
					System.out.println(postURL);

					System.out.println(messageJSON.toString());

					HTTPUtil.accessURLUsingPost(postURL, messageJSON.toString());
				}
			}
		}
		catch (Exception e)
		{
		}

	}
}
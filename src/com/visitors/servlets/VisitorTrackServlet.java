package com.visitors.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.livily.db.DBUtil;
import com.visitors.track.EventTrack;
import com.visitors.track.EventTrack.EVENT;
import com.visitors.track.EventTrackUtil;
import com.visitors.track.tasks.EventTrackDeferred;
import com.visitors.util.ClickDeskServletUtil;
import com.visitors.util.JSONUtil;
import com.visitors.util.Util;

@SuppressWarnings("serial")
public class VisitorTrackServlet extends HttpServlet
{

	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{

		try
		{

			resp.setContentType("application/x-javascript;charset=utf-8");

			String command = req.getParameter("command");
			if (StringUtils.isBlank(command))
				throw new Exception("Invalid command.");

			if (StringUtils.equalsIgnoreCase(command, EVENT.WEB_VISITOR.toString()))
			{
				logVisitorInfo(req, resp);
			}

			if (StringUtils.equalsIgnoreCase(command, EVENT.VISITOR_UPDATE.toString()))
			{
				updateVisitorInfo(req, resp);
			}

			if (StringUtils.equalsIgnoreCase(command, EVENT.GET_VISITOR_INFO.toString()))
			{
				getVisitorInfo(req, resp);
			}

			if (StringUtils.equalsIgnoreCase(command, "active-users"))
			{
				getChannelUsers(req, resp);
			}

			if (StringUtils.equalsIgnoreCase(command, "active-visitors-count"))
			{
				getChannelVisitorsCount(req, resp);
			}

		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			try
			{
				ClickDeskServletUtil.sendError(req, resp, e.getMessage());
			}
			catch (Exception e2)
			{
			}

		}

	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	public void getChannelVisitorsCount(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String channelName = req.getParameter(EventTrack.CHANNEL_NAME);
		if (StringUtils.isBlank(channelName))
			throw new Exception("Invalid params.");

		String widgetIds = req.getParameter("widget_ids");

		int count = EventTrack.getActiveChannelUsersCount(channelName, widgetIds);

		ClickDeskServletUtil.sendResponse(req, resp, new Integer(count));

	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	public void getChannelUsers(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{

		String channelName = req.getParameter(EventTrack.CHANNEL_NAME);
		if (StringUtils.isBlank(channelName))
			throw new Exception("Invalid params.");

		String cursor = req.getParameter("cursor");
		String pageSize = req.getParameter("page_size");

		String widgets = req.getParameter("widget_ids");

		JSONArray usersArray = EventTrack.getActiveChannelUsers(channelName, cursor, pageSize, widgets);

		/*
		 * JSONArray usersArray = EventsSQLUtil.getVisitorsFromChannel(new
		 * JSONObject().put("channel_name", channelName)
		 * .put(EventTrack.VISITOR_STATUS, STATUS.ACTIVE.toString()));
		 */

		JSONArray modifiedUsersArray = new JSONArray();
		for (int i = 0; i < usersArray.length(); i++)
		{

			JSONObject eachUserJSON = usersArray.getJSONObject(i);
			JSONObject marfedJSON = new JSONObject();
			try
			{
				Text visitorText = (Text) eachUserJSON.get(EventTrack.VISITOR_INFO);
				marfedJSON = new JSONObject(visitorText.getValue());
			}
			catch (Exception e)
			{
			}

			try
			{
				Text visitorText = (Text) eachUserJSON.get(EventTrack.VISITOR_PATH);
				marfedJSON.put(EventTrack.VISITOR_PATH, new JSONArray(visitorText.getValue()).toString());

			}
			catch (Exception e)
			{
			}

			// JSONObject marfedJSON = new
			// JSONObject(eachUserJSON.getString(EventTrack.VISITOR_INFO));

			if (eachUserJSON.has("cursor"))
				marfedJSON.put("cursor", JSONUtil.getJSONValue(eachUserJSON, "cursor"));

			if (eachUserJSON.has("total_count"))
				marfedJSON.put("total_count", JSONUtil.getJSONValue(eachUserJSON, "total_count"));

			modifiedUsersArray.put(marfedJSON);
		}

		System.out.println(modifiedUsersArray);

		ClickDeskServletUtil.sendResponse(req, resp, modifiedUsersArray);

	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	public void getVisitorInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String uuId = req.getParameter("uuid");

		// Get visitor info
		JSONObject visitorJSON = EventTrack.getLog(new JSONObject().put(EventTrack.UUID, uuId));
		if (visitorJSON == null)
			return;

		try
		{
			visitorJSON.put(EventTrack.VISITOR_INFO,
					new JSONObject(((Text) visitorJSON.get(EventTrack.VISITOR_INFO)).getValue()));

			visitorJSON.put(EventTrack.VISITOR_PATH,
					new JSONArray(((Text) visitorJSON.get(EventTrack.VISITOR_PATH)).getValue()));

		}
		catch (Exception e)
		{
		}

		ClickDeskServletUtil.sendResponse(req, resp, visitorJSON);

	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	public void updateVisitorInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{

		String uuId = req.getParameter("uuid");
		String name = req.getParameter("name");
		String email = req.getParameter("email");

		System.out.println("updateVisitorInfo " + uuId + " : " + name + " : " + email);

		if (StringUtils.isBlank(uuId))
			throw new Exception("Invalid params.");

		// Get visitor info
		JSONObject visitorJSON = EventTrack.getLog(new JSONObject().put(EventTrack.UUID, uuId));
		if (visitorJSON == null)
			return;

		Text visitorText = (Text) visitorJSON.get(EventTrack.VISITOR_INFO);
		JSONObject visitorInfoJSON = new JSONObject(visitorText.getValue());

		JSONObject userInfoJSON = new JSONObject();

		try
		{

			userInfoJSON = new JSONObject(visitorInfoJSON.getString("info"));
			JSONObject dbInfoJSON = userInfoJSON.getJSONObject("visitor_info");

			if (StringUtils.isNotBlank(name))
				dbInfoJSON.put("name", name);
			if (StringUtils.isNotBlank(email))
				dbInfoJSON.put("email", email);

			userInfoJSON.put("visitor_info", dbInfoJSON);

			try
			{
				Text visitorPathText = (Text) visitorJSON.get(EventTrack.VISITOR_PATH);
				userInfoJSON.put(EventTrack.VISITOR_PATH, new JSONArray(visitorPathText.getValue()).toString());
			}
			catch (Exception e)
			{
			}

			visitorInfoJSON.put("info", userInfoJSON);

		}
		catch (Exception e)
		{
			System.out.println(ExceptionUtils.getFullStackTrace(e));
		}

		System.out.println(visitorInfoJSON.toString());

		EventTrack.updateLog(DBUtil.getId(visitorJSON),
				new JSONObject().put(EventTrack.VISITOR_INFO, new Text(visitorInfoJSON.toString())));

		String channelName = uuId;

		// Send updated event to ClickDesk
		JSONObject messageJSON = new JSONObject().put("name", "member_updated");
		messageJSON.put("channel", channelName);
		messageJSON.put("member_id", userInfoJSON.toString());

		EventTrackUtil.sendEventLogToClickDesk(messageJSON, channelName);

	}

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	public void logVisitorInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{

		String visitorInfo = req.getParameter(EventTrack.VISITOR_INFO);
		String channelName = req.getParameter(EventTrack.CHANNEL_NAME);

		if (StringUtils.isBlank(visitorInfo) || StringUtils.isBlank(channelName))
			throw new Exception("Invalid params.");

		System.out.println(visitorInfo);
		System.out.println(channelName);

		if (Util.isChannelInBlockedList(channelName.substring(0, channelName.lastIndexOf("-"))))
			throw new Exception("Disabled Tracking.");

		JSONObject visitorJSON = new JSONObject(visitorInfo);

		JSONObject updatedNewJSON = new JSONObject();
		updatedNewJSON.put("id", visitorJSON.getString("visitorId"));
		updatedNewJSON.put("info", visitorJSON);

		// Log Info
		JSONObject logJSON = EventTrackUtil.createWebVisitorLog(updatedNewJSON, req);

		// JSONObject logJSON =
		// EventTrackUtil.createWebVisitorInsertQueryJSON(updatedNewJSON, req,
		// "%");

		// Add to deferred task for update db
		EventTrackDeferred statusDeffered = new EventTrackDeferred("login", logJSON.toString(), channelName);
		TaskOptions options = TaskOptions.Builder.withPayload(statusDeffered).header("HOST",
				BackendServiceFactory.getBackendService().getBackendAddress("track-test-app"));
		statusDeffered.run();
		/*Queue que = QueueFactory.getQueue("event-track-queue" + Util.randInt(1, 10));
		que.add(options); */

		// EventsLogUtil.addActiveStatusLogToMemcache(channelName, logJSON);

		// Send added event to ClickDesk
		JSONObject messageJSON = new JSONObject().put("name", "member_added");
		messageJSON.put("channel", channelName);
		messageJSON.put("member_id", updatedNewJSON.toString());

		try
		{
			if (logJSON.has(EventTrack.VISITOR_PATH))
				messageJSON.put(EventTrack.VISITOR_PATH, logJSON.getString(EventTrack.VISITOR_PATH));
		}
		catch (Exception e)
		{
		}

		EventTrackUtil.sendEventLogToClickDesk(messageJSON, channelName);
	}
}
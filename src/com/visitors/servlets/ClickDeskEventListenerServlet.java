package com.visitors.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.visitors.track.EventTrackUtil;
import com.visitors.track.tasks.EventTrackDeferred;
import com.visitors.util.Util;

@SuppressWarnings("serial")
public class ClickDeskEventListenerServlet extends HttpServlet
{
	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{

		try
		{

			resp.setContentType("text/plain;charset=UTF-8");
			ServletInputStream in = req.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String pusherMessage = "";
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				pusherMessage += (line);
			}

			System.out.println("pusherMessage = " + pusherMessage);

			// Construct JSON
			JSONObject pusherJSON = new JSONObject(pusherMessage);

			JSONObject eventJSON = pusherJSON.getJSONArray("events").getJSONObject(0);

			// Get widgetid from channel name
			String channelName = eventJSON.getString("channel");
			System.out.println("channelName = " + channelName);

			// Abort if it is status change channel
			if (channelName.contains("cd_widget-"))
				return;

			// Event Name
			String name = eventJSON.getString("name");
			System.out.println("name = " + name);

			if (StringUtils.isBlank(name) || !StringUtils.equalsIgnoreCase(name, "channel_vacated"))
				return;

			// Send vacated event to ClickDesk
			JSONObject messageJSON = new JSONObject().put("name", "member_removed");
			messageJSON.put("channel", channelName);

			// Reset visitor
			// String keyId = EventTrack.getLogDBID(new
			// JSONObject().put(EventTrack.UUID, channelName));
			// if (keyId == null)
			// return;

			// EventTrack.updateLog(keyId, new
			// JSONObject().put(EventTrack.VISITOR_STATUS,
			// STATUS.INACTIVE.toString()));

			// Add to deferred task for emails list

			EventTrackDeferred statusDeffered = new EventTrackDeferred("exit", messageJSON.toString(), channelName);
			TaskOptions options = TaskOptions.Builder.withPayload(statusDeffered).header("HOST",
					BackendServiceFactory.getBackendService().getBackendAddress("track-test-app"));
			statusDeffered.run();
			/*Queue que = QueueFactory.getQueue("event-track-queue" + Util.randInt(1, 10));
			que.add(options);
			*/
			EventTrackUtil.sendEventLogToClickDesk(messageJSON, channelName);

		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}

	}
}

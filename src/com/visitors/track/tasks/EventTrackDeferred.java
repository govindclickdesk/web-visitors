package com.visitors.track.tasks;

import org.json.JSONObject;

import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.visitors.track.EventTrack;
import com.visitors.track.EventTrack.STATUS;

public class EventTrackDeferred implements DeferredTask
{

	private String eventType;
	private String logJSONString;
	private String channelName;

	public EventTrackDeferred(String eventType, String log, String channel) throws Exception
	{
		this.eventType = eventType;
		this.logJSONString = log;
		this.channelName = channel;
	}

	@Override
	public void run()
	{

		try
		{
			JSONObject logJSON = new JSONObject(logJSONString);

			if (eventType.equalsIgnoreCase("login"))
			{
				try
				{
					if (logJSON.has(EventTrack.VISITOR_INFO))
					{
						logJSON.put(EventTrack.VISITOR_INFO, new Text(logJSON.getString(EventTrack.VISITOR_INFO)));
					}

					if (logJSON.has(EventTrack.VISITOR_PATH))
					{
						logJSON.put(EventTrack.VISITOR_PATH, new Text(logJSON.getString(EventTrack.VISITOR_PATH)));
					}

				}
				catch (Exception e)
				{
				}

				EventTrack.trackLog(channelName, logJSON);
			}
			else
			{

				// Reset visitor
				String keyId = EventTrack.getLogDBID(new JSONObject().put(EventTrack.UUID, channelName));
				if (keyId == null)
					return;

				EventTrack
						.updateLog(keyId, new JSONObject().put(EventTrack.VISITOR_STATUS, STATUS.INACTIVE.toString()));

			}

		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}

	}
}

package com.visitors.track.tasks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.livily.pusher.Pusher;
import com.visitors.track.EventTrack;
import com.visitors.track.EventTrack.STATUS;
import com.visitors.util.JSONUtil;

public class TimeoutDeferred implements DeferredTask
{

	private String channelName;

	public TimeoutDeferred(String userChannel)
	{
		this.channelName = userChannel;
	}

	@Override
	public void run()
	{

		System.out.println("channelName = " + channelName);

		if (StringUtils.isBlank(channelName))
			return;

		try
		{

			String resp_content = new String(Pusher.getChannelsList(channelName).getContent());

			JSONObject respJSON = new JSONObject(resp_content);
			JSONObject channels = respJSON.getJSONObject("channels");

			// Get active visitors
			JSONArray usersArray = EventTrack.getActiveChannelUsers(channelName, null, null, null);

			Set<String> visitorsSet = new HashSet<String>();
			for (int i = 0; i < usersArray.length(); i++)
			{
				JSONObject eachVisitor = usersArray.getJSONObject(i);
				visitorsSet.add(JSONUtil.getJSONValue(eachVisitor, EventTrack.UUID));
			}

			for (Iterator iterator = visitorsSet.iterator(); iterator.hasNext();)
			{

				String channel = (String) iterator.next();
				if (channels.has(channel))
					continue;

				// Set offline status
				String keyId = EventTrack.getLogDBID(new JSONObject().put(EventTrack.UUID, channel).put(
						EventTrack.VISITOR_STATUS, STATUS.ACTIVE.toString()));
				if (keyId == null)
					continue;

				EventTrack
						.updateLog(keyId, new JSONObject().put(EventTrack.VISITOR_STATUS, STATUS.INACTIVE.toString()));

			}

		}
		catch (Exception e)
		{
			System.out.println(ExceptionUtils.getFullStackTrace(e));
		}

	}
	
	public static void main(String[] args) {
		System.out.println(new String(Pusher.getChannelsList("1vmqh2").getContent()));
	}
}
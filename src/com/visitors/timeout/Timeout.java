package com.visitors.timeout;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.livily.db.DBUtil;
import com.visitors.track.EventTrack;
import com.visitors.track.EventTrack.STATUS;
import com.visitors.track.tasks.RunTimeoutDeamonTask;
import com.visitors.track.tasks.TimeoutDeferred;
import com.visitors.util.JSONUtil;

public class Timeout {

	public static final Long VISITOR_TIME_OUT_IN_SECS = 3600 * 2L;

	// Get Old Sessions
	public static JSONArray getOldVisitors(Integer offset, Integer pageSize) throws Exception {

		// Query
		Query query = getOldVisitorsQuery();

		if (offset != null && pageSize != null) {
			return DBUtil.executeQueryWithLimitAndOffset(query, offset, pageSize);
		}

		// Old Sessions
		return DBUtil.executeQueryWithLimit(query, 2000);

	}

	// Get Old Sessions
	public static int getOldVisitorsCount() throws Exception {

		// Old Sessions
		return DBUtil.executeQueryCount(getOldVisitorsQuery(), 100000);

	}

	/**
	 * Creates Query for old visitors
	 * 
	 * @param offset
	 * @param pageSize
	 * @return
	 */
	static Query getOldVisitorsQuery() {

		// Get all visitors with last_entry_time
		long timeOutMilliSeconds = Calendar.getInstance().getTimeInMillis() - VISITOR_TIME_OUT_IN_SECS * 1000;

		// Query
		Query query = new Query(EventTrack.DB_NAME);

		// Find dbObjects which are old and active
		query.addFilter(EventTrack.VISITOR_STATUS, Query.FilterOperator.EQUAL, STATUS.ACTIVE.toString());

		// Add timeout
		query.addFilter(EventTrack.DATE, Query.FilterOperator.LESS_THAN, timeOutMilliSeconds);

		return query;
	}

	/**
	 * Gets list of channelNames
	 * 
	 * @param oldVisitors
	 * @return
	 */
	public static Set<String> getChannelSet(JSONArray oldVisitors) {
		Set<String> visitorsSet = new HashSet<String>();
		for (int i = 0; i < oldVisitors.length(); i++) {
			try {
				JSONObject eachVisitor = oldVisitors.getJSONObject(i);
				visitorsSet.add(JSONUtil.getJSONValue(eachVisitor, EventTrack.CHANNEL_NAME));
			} catch (Exception e) {
			}

		}

		return visitorsSet;
	}

	/**
	 * 
	 * @param visitorsSet
	 */
	public static void createTimeOutTask(Set<String> visitorsSet) {
		System.out.println(visitorsSet.size());

		for (String string : visitorsSet) {
			// Add to deferred task for emails list
			TimeoutDeferred statusDeffered = new TimeoutDeferred(string);

			TaskOptions options = TaskOptions.Builder.withPayload(statusDeffered);

			// Set Retry Limit to zero
			options.retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));
			Queue que = QueueFactory.getQueue("timeout-visitor-queue");
			que.add(options);

		}
	}

	/**
	 * 
	 * @param visitorsSet
	 */
	public static void createTimeOutRangeDeamon(Integer offset, Integer listSize) {

		// Add to deferred task for emails list
		RunTimeoutDeamonTask deamonDeffered = new RunTimeoutDeamonTask(offset, listSize);
		TaskOptions options = TaskOptions.Builder.withPayload(deamonDeffered);

		// Set Retry Limit to zero
		options.retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));
		Queue que = QueueFactory.getQueue("timeout-visitor-queue");
		que.add(options);
	}

	public static void runDeamon(Integer offset, Integer pageSize) {
		try {
			JSONArray oldVisitors = getOldVisitors(offset, pageSize);
			Set<String> visitorsSet = getChannelSet(oldVisitors);
			System.out.println(visitorsSet);
			
			createTimeOutTask(visitorsSet);
		} catch (Exception e) {
			System.out.println(ExceptionUtils.getFullStackTrace(e));
		}

	}
}

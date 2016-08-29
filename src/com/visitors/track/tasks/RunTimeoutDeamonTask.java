package com.visitors.track.tasks;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.visitors.timeout.Timeout;

public class RunTimeoutDeamonTask implements DeferredTask {

	private Integer offset;
	private Integer listSize;

	public RunTimeoutDeamonTask(Integer offset, Integer listSize) {
		this.offset = offset;
		this.listSize = listSize;
	}

	@Override
	public void run() {
		try {
			Timeout.runDeamon(offset, listSize);
		} catch (Exception e) {
			System.out.println(ExceptionUtils.getFullStackTrace(e));
		}

	}
}
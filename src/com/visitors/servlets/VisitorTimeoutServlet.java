package com.visitors.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.livily.pusher.Pusher;
import com.visitors.timeout.Timeout;
import com.visitors.util.ClickDeskServletUtil;

@SuppressWarnings("serial")
public class VisitorTimeoutServlet extends HttpServlet {
	private static final int SUBLIST_LENGTH = 300;

	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		try {
			resp.setContentType("application/x-javascript;charset=utf-8");
			
			// Clean only selected channel visitors
			if(req.getParameter("channel") != null) {
				String channelName = req.getParameter("channel");
				resp.getWriter().println(new String(Pusher.getChannelsList(channelName).getContent()));
				
				
				return;
			}

			int visitorsCount = Timeout.getOldVisitorsCount();
			resp.getWriter().println(visitorsCount + "");

			for (int i = 0; i < visitorsCount / SUBLIST_LENGTH; i++) {
				Timeout.createTimeOutRangeDeamon(i * SUBLIST_LENGTH, SUBLIST_LENGTH);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			try {
				ClickDeskServletUtil.sendError(req, resp, e.getMessage());
			} catch (Exception e2) {
			}

		}

	}

}

package com.visitors.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public class ClickDeskServletUtil
{
	// Response
	public final static String SERVER_RESPONSE_STATUS = "status";

	// Response = Success Code
	public final static String SERVER_RESPONSE_STATUS_SUCCESS = "success";
	public final static String SERVER_RESPONSE_STATUS_RESPONSE = "response";

	// Response - Failure Codes
	public final static String SERVER_RESPONSE_STATUS_ERROR = "error";

	// Invalid Responses
	public final static String SERVER_RESPONSE_ERROR_INVALID_PARAMETERS = "Invalid parameters for command - ";
	public final static String SERVER_RESPONSE_STATUS_ERROR_MESSG = "errormssg";

	// Get Success JSON
	public static JSONObject getSuccessJSON(Object response) throws Exception
	{

		return new JSONObject().put(ClickDeskServletUtil.SERVER_RESPONSE_STATUS,
				ClickDeskServletUtil.SERVER_RESPONSE_STATUS_SUCCESS).put(
				ClickDeskServletUtil.SERVER_RESPONSE_STATUS_RESPONSE, response);
	}

	// Get Success JSON
	public static JSONObject getSuccessJSON(String responseString) throws Exception
	{

		return new JSONObject().put(ClickDeskServletUtil.SERVER_RESPONSE_STATUS,
				ClickDeskServletUtil.SERVER_RESPONSE_STATUS_SUCCESS).put(
				ClickDeskServletUtil.SERVER_RESPONSE_STATUS_RESPONSE, responseString);
	}

	// Send Error as JSON
	public static void sendError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
			throws Exception
	{

		String jsonMessage = ClickDeskServletUtil.getErrorJSON(errorMessage).toString();

		// Send Response - jSON or JSONP
		String callback = req.getParameter("callback");
		if (callback == null)
			resp.getWriter().println(jsonMessage);
		else
		{
			resp.getWriter().println(callback + "(" + jsonMessage + ")");
		}
	}

	// Check Parameters
	public static void checkParameters(HttpServletRequest req, HttpServletResponse resp, String[] parameters)
			throws Exception
	{
		// Check if there are all parameters
		for (int index = 0; index < parameters.length; index++)
		{
			String parameter = req.getParameter(parameters[index]);
			if (parameter == null || parameter.length() == 0)
			{
				throw new Exception("Invalid parameters for command - " + " " + parameters[index] + " Not found.");
			}
		}
	}

	// Send Response
	public static void sendResponse(HttpServletRequest req, HttpServletResponse resp, Object response) throws Exception
	{

		String jsonMessage = getSuccessJSON(response).toString();

		// Send Response - jSON or JSONP
		String callback = req.getParameter("callback");
		if (callback == null)
			resp.getWriter().println(jsonMessage);
		else
		{
			resp.getWriter().println(callback + "(" + jsonMessage + ")");
		}

	}

	// Send OK Response
	public static void sendSuccessResponse(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String jsonMessage = new JSONObject().put(SERVER_RESPONSE_STATUS, SERVER_RESPONSE_STATUS_SUCCESS).toString();

		// Send Response - jSON or JSONP
		String callback = req.getParameter("callback");
		if (callback == null)
			resp.getWriter().println(jsonMessage);
		else
		{
			resp.getWriter().println(callback + "(" + jsonMessage + ")");
		}
	}

	// Get Error JSON
	public static JSONObject getErrorJSON(String errorMessage) throws Exception
	{

		return new JSONObject().put(SERVER_RESPONSE_STATUS, SERVER_RESPONSE_STATUS_ERROR).put(
				SERVER_RESPONSE_STATUS_ERROR_MESSG, errorMessage);
	}

}

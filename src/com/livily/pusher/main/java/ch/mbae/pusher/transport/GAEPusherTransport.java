package com.livily.pusher.main.java.ch.mbae.pusher.transport;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.livily.pusher.main.java.ch.mbae.pusher.PusherResponse;
import com.livily.pusher.main.java.ch.mbae.pusher.PusherTransport;
import com.livily.pusher.main.java.ch.mbae.pusher.PusherTransportException;

/**
 * Transport implementation for GAE.
 * 
 * @author Stephan Scheuermann Copyright 2010. Licensed under the MIT license:
 *         http://www.opensource.org/licenses/mit-license.php
 */
public class GAEPusherTransport implements PusherTransport
{

	@Override
	public PusherResponse fetch(URL url, String jsonData) throws PusherTransportException
	{
		// Create Google APP Engine Fetch URL service and request
		URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
		HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Content-Type", "application/json"));
		request.setPayload(jsonData.getBytes());

		// Start request
		try
		{
			HTTPResponse httpResponse = urlFetchService.fetch(request);
			PusherResponse response = new PusherResponse();
			response.setContent(httpResponse.getContent());
			response.setResponseCode(httpResponse.getResponseCode());
			response.setHeaders(this.extractHeaders(httpResponse));

			return response;
		}
		catch (IOException e)
		{
			throw new PusherTransportException("exception while POSTing payload using GAE transport", e);
		}
	}

	/**
	 * copies GAE headers to string map
	 * 
	 * @param httpResponse
	 *            the GAU response
	 * @return a <code>Map<String,String></code> containing the http headers
	 */
	private Map<String, String> extractHeaders(HTTPResponse httpResponse)
	{
		Map<String, String> headers = new HashMap<String, String>();
		for (HTTPHeader header : httpResponse.getHeaders())
		{
			headers.put(header.getName(), header.getValue());
		}
		return headers;
	}

}

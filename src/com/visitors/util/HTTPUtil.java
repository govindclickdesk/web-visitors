package com.visitors.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class HTTPUtil
{

	// Post request to URL
	public static String accessURLUsingPost(String postURL, String data) throws Exception
	{
		// Send data
		URL url = new URL(postURL);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);

		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
		wr.write(data);
		wr.flush();

		// Get the response
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String output = "";
		String inputLine;
		while ((inputLine = reader.readLine()) != null)
		{
			output += inputLine;
		}

		wr.close();
		reader.close();
		return output;
	}

}

package com.visitors.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Util
{

	public static List<String> blockedList = new ArrayList<String>();
	static
	{
		blockedList.add("1MTKA");
		blockedList.add("g93uq");
		// blockedList.add("64JUS");
	}

	public static boolean isChannelInBlockedList(String channelName)
	{

		return blockedList.contains(channelName);

	}

	/**
	 * Returns a pseudo-random number between min and max, inclusive. The
	 * difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 * 
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static int randInt(int min, int max)
	{

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;

	}

	public static void main(String[] args) throws Exception
	{

		HashMap map = new HashMap();
		for (int i = 0; i < 1000; i++)
		{

			if (i == 50)
				Thread.sleep(200);

			int j = randInt(1, 15);

			if (map.containsKey(j))
			{
				int k = (Integer) map.get(j);
				map.put(j, ++k);
			}
			else
				map.put(j, 1);
		}

		System.out.println(map);
	}
}

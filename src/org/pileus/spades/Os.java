package org.pileus.spades;

import android.util.Log;

public class Os
{
	/* Debugging */
	public static void debug(String txt, Exception e)
	{
		Log.d("Spades", txt, e);
	}
	public static void debug(String txt)
	{
		Log.d("Spades", txt);
	}
}

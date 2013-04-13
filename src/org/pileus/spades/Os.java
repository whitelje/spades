package org.pileus.spades;

import android.util.Log;
import android.util.Base64;

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

	/* Utilities */
	public static String base64(String txt)
	{
		return Base64.encodeToString(txt.getBytes(), 0);
	}
}

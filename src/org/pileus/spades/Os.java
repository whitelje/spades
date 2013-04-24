package org.pileus.spades;

import android.util.Log;
import android.util.Base64;
import android.graphics.Color;

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

	public static int getColor(String hex)
	{
		return Color.parseColor("#"+hex);
	}
}

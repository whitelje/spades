package org.pileus.spades;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.util.Log;

public class Message
{
	/* Constnats */
	private final  String  reMsg  = "(:([^ ]+) +)?(([A-Z0-9]+) +)(([^ ]+) +)?(([^: ]+) +)?(:(.*))";
	private final  String  reFrom = "([^! ]+)!";
	private final  String  reTo   = "(([^ :,]*)[:,] *)?(.*)";

	private static Pattern ptMsg  = null;
	private static Pattern ptFrom = null;
	private static Pattern ptTo   = null;

	/* Public data */
	public String  line = "";

	public String  src  = "";
	public String  cmd  = "";
	public String  dst  = "";
	public String  arg  = "";
	public String  msg  = "";

	public String  from = "";
	public String  to   = "";
	public String  txt  = "";

	/* Private methods */
	private String notnull(String string)
	{
		return string == null ? "" : string;
	}

	/* Public Methods */
	public Message(String line)
	{

		if (ptMsg  == null) ptMsg  = Pattern.compile(reMsg);
		if (ptFrom == null) ptFrom = Pattern.compile(reFrom);
		if (ptTo   == null) ptTo   = Pattern.compile(reTo);

		line = line.replaceAll("\\s+",       " ");
		line = line.replaceAll("^ | $",      "");
		line = line.replaceAll("\003[0-9]*", "");
		this.line = line;

		Matcher mrMsg = ptMsg.matcher(line);
		if (mrMsg.matches()) {
			this.src  = notnull(mrMsg.group(2));
			this.cmd  = notnull(mrMsg.group(4));
			this.dst  = notnull(mrMsg.group(6));
			this.arg  = notnull(mrMsg.group(8));
			this.msg  = notnull(mrMsg.group(10));
		}

		Matcher mrFrom = ptFrom.matcher(this.src);
		if (mrFrom.matches())
			this.from = notnull(mrFrom.group(1));

		Matcher mrTo = ptTo.matcher(this.msg);
		if (mrTo.matches())
			this.to   = notnull(mrTo.group(2));

		if (this.to.equals(""))
			this.txt  = notnull(this.msg);
		else
			this.txt  = notnull(mrTo.group(3));
	}

	public void debug()
	{
		Log.d("Spades", "---------------------");
		Log.d("Spades", "line = [" + line + "]");
		Log.d("Spades", "src  = " + this.src);
		Log.d("Spades", "cmd  = " + this.cmd);
		Log.d("Spades", "dst  = " + this.dst);
		Log.d("Spades", "arg  = " + this.arg);
		Log.d("Spades", "msg  = " + this.msg);
		Log.d("Spades", "from = " + this.from);
		Log.d("Spades", "to   = " + this.to);
		Log.d("Spades", "txt  = " + this.txt);
		Log.d("Spades", "---------------------");
	}
}

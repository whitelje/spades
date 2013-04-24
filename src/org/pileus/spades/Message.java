package org.pileus.spades;

import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Message
{
	/* Enumerations */
	static enum Type {
		OTHER,    // Unknown message type
		JOIN,     // Join channel
		PART,     // Leave channel
		PRIVMSG,  // Private message
		TOPIC,    // Display current topic
		NAMES,    // Display user names
		ERROR,    // Error message from server
		CAP,      // Server capabilities
		AUTH,     // Authentication message
		AUTHOK,   // Authentication succeeded
		AUTHFAIL, // Authentication failed
	};

	static enum How {
		OTHER,    // Unknown message type
		CHANNEL,  // Normal message to a channel
		MENTION,  // User was mentioned in message text
		DIRECT,   // Message directed towards user
		PRIVMSG,  // Private message to user only
		SENT      // Message was sent by the user
	};

	/* Constants */
	private static final String  reMsg  = "(:([^ ]+) +)?(([A-Z0-9]+) +)(([^ ]+)[= ]+)?(([^: ]+) *)?(:(.*))?";
	private static final String  reFrom = "([^! ]+)!.*";
	private static final String  reTo   = "(([^ :,]*)[:,] *)?(.*)";
	private static final String  reCmd  = "/([a-z]+)( +(.*))?";

	private static final Pattern ptMsg  = Pattern.compile(reMsg);
	private static final Pattern ptFrom = Pattern.compile(reFrom);
	private static final Pattern ptTo   = Pattern.compile(reTo);
	private static final Pattern ptCmd  = Pattern.compile(reCmd);

	/* Public data */
	public Date    time = null;

	public String  line = "";

	public String  src  = "";
	public String  cmd  = "";
	public String  dst  = "";
	public String  arg  = "";
	public String  msg  = "";

	public Type    type = Type.OTHER;
	public How     how  = How.OTHER;
	public String  from = "";
	public String  to   = "";
	public String  txt  = "";

	/* Static methods */
	public static String clean(String msg)
	{
		String num = "0?[0-9]|1[0-5]";
		return msg.replaceAll("[\\002\\011\\017\\025]", "")
		          .replaceAll("[\\003\\013]("+num+")(,"+num+")?", "");
	}

	/* Public Methods */
	public Message(String dst, String from, String msg)
	{
		this.time = new Date();
		this.how  = How.SENT;
		this.from = from;

		if (this.parseSlash(msg))
			return;

		this.type = Type.PRIVMSG;
		this.cmd  = "PRIVMSG";
		this.dst  = dst;
		this.msg  = msg;
		this.line = this.cmd + " " + this.dst + " :" + this.msg;
	}

	public Message(String line, String name)
	{
		this.time = new Date();

		this.parseText(line);
		this.parseTypes(name);
	}

	public void debug()
	{
		Os.debug("---------------------");
		Os.debug("line = [" + line + "]");
		Os.debug("src  = " + this.src);
		Os.debug("cmd  = " + this.cmd);
		Os.debug("dst  = " + this.dst);
		Os.debug("arg  = " + this.arg);
		Os.debug("msg  = " + this.msg);
		Os.debug("from = " + this.from);
		Os.debug("to   = " + this.to);
		Os.debug("txt  = " + this.txt);
		Os.debug("---------------------");
	}

	public String toString()
	{
		return this.from + ": " + this.txt;
	}

	/* Private methods */
	private String notnull(String string)
	{
		return string == null ? "" : string;
	}

	private boolean parseSlash(String msg)
	{
		if (msg.charAt(0) != '/')
			return false;

		// Split up line
		Matcher mr = ptCmd.matcher(msg);
		if (!mr.matches())
			return false;

		String cmd = notnull(mr.group(1));
		String arg = notnull(mr.group(3));

		// Parse commands
		if (cmd.matches("join")) {
			Os.debug("Message: /join");
			this.type = Type.JOIN;
			this.cmd  = "JOIN";
			this.msg  = arg;
			this.line = this.cmd + " :" + arg;
		}

		if (cmd.matches("part")) {
			Os.debug("Message: /part");
			this.type = Type.PART;
			this.cmd  = "PART";
			this.msg  = arg;
			this.line = this.cmd + " :" + arg;
		}

		// Print warning if command is not recognized
		if (this.line == null)
			Os.debug("Message: unknown command");

		return true;
	}

	private void parseText(String line)
	{
		// Cleanup line
		line = line.replaceAll("\\s+",       " ");
		line = line.replaceAll("^ | $",      "");
		this.line = line;

		// Split line into parts
		Matcher mrMsg = ptMsg.matcher(line);
		if (mrMsg.matches()) {
			this.src  = notnull(mrMsg.group(2));
			this.cmd  = notnull(mrMsg.group(4));
			this.dst  = notnull(mrMsg.group(6));
			this.arg  = notnull(mrMsg.group(8));
			this.msg  = notnull(mrMsg.group(10));
		}

		// Determine friendly parts
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

	private void parseTypes(String name)
	{
		// Parse commands names
		if      (this.cmd.equals("PRIVMSG"))       this.type = Type.PRIVMSG;
		else if (this.cmd.equals("332"))           this.type = Type.TOPIC;
		else if (this.cmd.equals("353"))           this.type = Type.NAMES;
		else if (this.cmd.equals("ERROR"))         this.type = Type.ERROR;
		else if (this.cmd.equals("CAP"))           this.type = Type.CAP;
		else if (this.cmd.equals("AUTHENTICATE"))  this.type = Type.AUTH;
		else if (this.cmd.equals("903"))           this.type = Type.AUTHOK;
		else if (this.cmd.equals("904"))           this.type = Type.AUTHFAIL;
		else if (this.cmd.equals("905"))           this.type = Type.AUTHFAIL;
		else if (this.cmd.equals("906"))           this.type = Type.AUTHFAIL;
		else if (this.cmd.equals("907"))           this.type = Type.AUTHFAIL;

		// Set directed
		if      (this.dst.equals(name))            this.how  = How.PRIVMSG;
		else if (this.to.equals(name))             this.how  = How.DIRECT;
		else if (this.msg.contains(name))          this.how  = How.MENTION;
		else if (this.type == Type.PRIVMSG)        this.how  = How.CHANNEL;
	}
}

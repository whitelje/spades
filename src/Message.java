package org.pileus.spades;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

	/* Structures */
	static class Color {
		public int     code;
		public String  hex;
		public String  name;
		public int     color;

		public Color(int code, String hex, String name)
		{
			this.code  = code;
			this.hex   = hex;
			this.name  = name;
			this.color = Os.getColor(hex);
		}
	};

	static class Format implements Cloneable {
		public boolean bold;
		public boolean italic;
		public boolean strike;
		public boolean underline;
		public boolean reverse;
		public Color   fg;
		public Color   bg;
		public String  txt;

		public Format clone()
		{
			try {
				return (Format)super.clone();
			} catch (Exception e) {
				return null;
			}
		}

		public String toString()
		{
			return (fg!=null  ? fg.hex : "xxxxxx") + ":" +
			       (bg!=null  ? bg.hex : "xxxxxx") + ":" +
			       (bold      ? "b"    : "-"     ) +
			       (italic    ? "i"    : "-"     ) +
			       (strike    ? "s"    : "-"     ) +
			       (underline ? "u"    : "-"     ) +
			       (reverse   ? "r"    : "-"     ) + ":" +
			       "[" + txt + "]";
		}
	};

	/* Colors */
	private static final Color colors[] = {
		new Color(0x0, "FFFFFF", "White"),
		new Color(0x1, "000000", "Black"),
		new Color(0x2, "000080", "Navy Blue"),
		new Color(0x3, "008000", "Green"),
		new Color(0x4, "FF0000", "Red"),
		new Color(0x5, "804040", "Brown"),
		new Color(0x6, "8000FF", "Purple"),
		new Color(0x7, "808000", "Olive"),
		new Color(0x8, "FFFF00", "Yellow"),
		new Color(0x9, "00FF00", "Lime Green"),
		new Color(0xA, "008080", "Teal"),
		new Color(0xB, "00FFFF", "Aqua Light"),
		new Color(0xC, "0000FF", "Royal Blue"),
		new Color(0xD, "FF00FF", "Hot Pink"),
		new Color(0xE, "808080", "Dark Gray"),
		new Color(0xF, "C0C0C0", "Light Gray"),
	};

	/* Constants */
	private static final String  reMsg  = "(:([^ ]+) +)?(([A-Z0-9]+) +)(([^ ]+)[= ]+)?(([^: ]+) *)?(:(.*))?";
	private static final String  reFrom = "([^! ]+)!.*";
	private static final String  reTo   = "(([^ :,]*)[:,] *)?(.*)";
	private static final String  reCmd  = "/([a-z]+)(?: +([^ ]*)(?: +(.*))?)?";

	private static final Pattern ptMsg  = Pattern.compile(reMsg);
	private static final Pattern ptFrom = Pattern.compile(reFrom);
	private static final Pattern ptTo   = Pattern.compile(reTo);
	private static final Pattern ptCmd  = Pattern.compile(reCmd);

	private static final String  cReNum = "1[0-5]|0?[0-9]";
	private static final String  cReFmt = "[\\002\\011\\017\\023\\025\\026\\037]";
	private static final String  cReClr = "[\\003\\013]("+cReNum+")?(,("+cReNum+"))?";
	private static final String  cRegex = cReFmt + "|" + cReClr + "|$";
	private static final Pattern cPtrn  = Pattern.compile(cRegex);

	/* Public data */
	public Date    time = null;        // Message delivery time

	public String  line = "";          // Raw IRC line     -- george@~g@example.com PRIVMSG #chat :larry: hello!

	public String  src  = "";          // IRC Source       -- george!~g@example.com
	public String  cmd  = "";          // IRC Command      -- PRIVMSG
	public String  dst  = "";          // IRC Destination  -- #chat
	public String  arg  = "";          // IRC Arguments    -- #chan in topic msg, etc
	public String  msg  = "";          // IRC Message text -- larry: Hello!

	public Type    type = Type.OTHER;  // Message Type     -- PRIVMSG
	public How     how  = How.OTHER;   // How msg relates  -- SENT=geroge, DIRECT=larry, CHANNEL=*
	public String  from = "";          // Nick of sender   -- george
	public String  to   = "";          // Addressed name   -- larry
	public String  txt  = "";          // Text of msg      -- Hello!

	public List<Format> parts = new ArrayList<Format>();

	/* Static methods */
	private static Color getColor(String code)
	{
		if (code == null)
			return null;
		int i = Integer.parseInt(code);
		if (i >= 0 && i < Message.colors.length)
			return Message.colors[i];
		return null;
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
		this.parseColors(this.msg);
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
		String arg = notnull(mr.group(2));
		String txt = notnull(mr.group(3));

		// Parse commands
		if (cmd.matches("j(oin)?")) {
			Os.debug("Message: /join");
			this.type = Type.JOIN;
			this.cmd  = "JOIN";
			this.msg  = arg;
			this.line = this.cmd + " :" + arg;
		}

		if (cmd.matches("p(art)?")) {
			Os.debug("Message: /part");
			this.type = Type.PART;
			this.cmd  = "PART";
			this.msg  = arg;
			this.line = this.cmd + " :" + arg;
		}

		if (cmd.matches("msg") && arg != null) {
			Os.debug("Message: /msg");
			this.type = Type.PRIVMSG;
			this.cmd  = "PRIVMSG";
			this.dst  = arg;
			this.msg  = txt;
			this.line = this.cmd + " " + arg + " :" + txt;
		}

		// Print warning if command is not recognized
		if (this.line == null)
			Os.debug("Message: unknown command");

		return true;
	}

	private void parseText(String line)
	{
		// Cleanup line
		line = line.replaceAll("\\s+",  " ");
		line = line.replaceAll("^ | $", "");
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

	private void parseColors(String msg)
	{
		// Setup regex matching
		Matcher match = Message.cPtrn.matcher(msg);

		// Initialize state variables
		int    pos = 0;
		Format fmt = new Format();
		ArrayList<Format> list = new ArrayList<Format>();

		// Parse the string
		while (match.find()) {
			// Push current string
			fmt.txt = msg.substring(pos, match.start());
			if (fmt.txt.length() > 0)
				list.add(fmt.clone());
			pos = match.end();

			// Abort at end of string
			if (match.hitEnd())
				break;

			// Update format for next string
			switch (match.group().charAt(0)) {
				// Format attributes
				case 002: fmt.bold      ^= true; break;
				case 011: fmt.italic    ^= true; break;
				case 023: fmt.strike    ^= true; break;
				case 025: fmt.underline ^= true; break;
				case 037: fmt.underline ^= true; break;
				case 026: fmt.reverse   ^= true; break;

				// Reset
				case 017:
					  fmt = new Format();
					  break;

				// Colors
				case 003:
					  String fg = match.group(1);
					  String bg = match.group(3);
					  fmt.fg = Message.getColor(fg);
					  fmt.bg = Message.getColor(bg);
					  break;
			}
		}

		// Cleanup extra space
		list.trimToSize();
		this.parts = list;
		this.msg   = this.msg.replaceAll(cRegex, "");
		this.to    = this.to.replaceAll(cRegex, "");
		this.txt   = this.txt.replaceAll(cRegex, "");
	}
}

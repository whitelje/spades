package org.pileus.spades;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class Client
{
	/* Private data */
	private String         server   = null;
	private String         nickname = null;
	private String         channel  = null;
	private String         username = null;
	private String         hostname = null;

	private BufferedReader input    = null;
	private PrintWriter    output   = null;

	/* Public data */
	public  boolean        running  = true;

	/* Public Methods */
	public Client(String server, String nickname, String channel,
			String username, String hostname)
	{
		this.server   = server;
		this.nickname = nickname;
		this.channel  = channel;
		this.username = username;
		this.hostname = hostname;
		Os.debug("Client: create");
	}

	public Client(String server, String nickname, String channel)
	{
		this(server, nickname, channel, "user", "localhost");
	}

	public void connect(BufferedReader input, PrintWriter output)
	{
		this.input  = input;
		this.output = output;
		Os.debug("Client: connect");
		putline("USER "+username+" "+hostname+" "+server+" :"+nickname);
		putline("NICK "+nickname);
	}

	public Message send(String txt)
	{
		Message msg = new Message(channel, nickname, txt);
		putline(msg.line);
		return msg;
	}

	public Message recv()
	{
		try {
			String line = getline();
			if (line == null) {
				this.running = false;
				return null;
			} else {
				Message msg = new Message(line);
				process(msg);
				return msg;
			}
		} catch (Exception e) {
			Os.debug("Client: error in recv", e);
			this.running = false;
			return null;
		}
	}

	/* Private methods */
	private void process(Message msg)
	{
		if (msg.cmd.equals("001") && msg.msg.matches("Welcome.*")) {
			putline("JOIN "  + channel);
			putline("TOPIC " + channel);
		}
		if (msg.cmd.equals("PING")) {
			putline("PING " + msg.msg);
		}
	}

	private String getline()
	{
		try {
			String line = input.readLine();
			if (line != null)
				Os.debug("> " + line);
			return line;
		} catch (Exception e) {
			Os.debug("Client: error reading line", e);
			this.running = false;
			return "";
		}
	}

	private void putline(String line)
	{
		try {
			Os.debug("< " + line);
			output.println(line);
			output.flush();
		} catch (Exception e) {
			Os.debug("Client: error writing line", e);
			this.running = false;
		}
	}
}

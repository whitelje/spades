package org.pileus.spades;

import java.io.*;
import java.net.*;

public class Client
{
	/* Private data */
	private String         server   = null;
	private int            port     = 6667;
	private String         nickname = null;
	private String         channel  = null;
	private String         username = null;
	private String         hostname = null;

	private Socket         socket   = null;
	private BufferedReader input    = null;
	private PrintWriter    output   = null;

	/* Public data */
	public  boolean        ready    = false;

	/* Public Methods */
	public Client(String username, String hostname)
	{
		this.username = username;
		this.hostname = hostname;
		Os.debug("Client: create");
	}

	public Client()
	{
		this("user", "localhost");
	}

	public boolean connect(String server, String nickname, String channel)
	{
		Os.debug("Client: connect");

		this.server   = server;
		this.nickname = nickname;
		this.channel  = channel;

		try {
			this.socket = new Socket(this.server, this.port);
			this.input  = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.output = new PrintWriter(this.socket.getOutputStream());
		} catch (Exception e) {
			Os.debug("Client: failed to create connection: " + e);
			return false;
		}

		Os.debug("Client: connected");
		this.raw("USER "+this.username+" "+this.hostname+" "+this.server+" :"+this.nickname);
		this.raw("NICK "+this.nickname);

		return true;
	}

	public boolean abort()
	{
		Os.debug("Client: abort");
		try {
			this.socket.close();
			this.ready = false;
			return true;
		} catch (Exception e) {
			Os.debug("Client: error closing socket", e);
			return false;
		}
	}

	public void raw(String line)
	{
		try {
			Os.debug("< " + line);
			this.output.println(line);
			this.output.flush();
		} catch (Exception e) {
			Os.debug("Client: error writing line", e);
		}
	}

	public Message send(String txt)
	{
		Message msg  = new Message(this.channel, this.nickname, txt);
		this.raw(msg.line);
		return msg;
	}

	public Message recv()
	{
		try {
			String line = input.readLine();
			if (line == null)
				return null;
			Os.debug("> " + line);
			Message msg = new Message(line);
			this.process(msg);
			return msg;
		} catch (SocketException e) {
			this.ready = false;
			return null;
		} catch (Exception e) {
			this.ready = false;
			Os.debug("Client: error in recv", e);
			return null;
		}
	}

	/* Private methods */
	private void process(Message msg)
	{
		if (msg.cmd.equals("001") && msg.msg.matches("Welcome.*")) {
			this.raw("JOIN "  + this.channel);
			this.raw("TOPIC " + this.channel);
			this.ready = true;
		}
		if (msg.cmd.equals("PING")) {
			this.raw("PING " + msg.msg);
		}
	}
}

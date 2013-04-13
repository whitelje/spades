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
			socket = new Socket(this.server, this.port);
			input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream());
		} catch (Exception e) {
			Os.debug("Client: failed to create connection: " + e);
			return false;
		}

		Os.debug("Client: connected");
		raw("USER "+username+" "+hostname+" "+server+" :"+nickname);
		raw("NICK "+nickname);

		return true;
	}

	public boolean abort()
	{
		Os.debug("Client: abort");
		try {
			this.socket.close();
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
			output.println(line);
			output.flush();
		} catch (Exception e) {
			Os.debug("Client: error writing line", e);
		}
	}

	public Message send(String txt)
	{
		Message msg  = new Message(channel, nickname, txt);
		raw(msg.line);
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
			process(msg);
			return msg;
		} catch (SocketException e) {
			return null;
		} catch (Exception e) {
			Os.debug("Client: error in recv", e);
			return null;
		}
	}

	/* Private methods */
	private void process(Message msg)
	{
		if (msg.cmd.equals("001") && msg.msg.matches("Welcome.*")) {
			raw("JOIN "  + channel);
			raw("TOPIC " + channel);
		}
		if (msg.cmd.equals("PING")) {
			raw("PING " + msg.msg);
		}
	}
}

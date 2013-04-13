package org.pileus.spades;

import java.io.*;
import java.net.*;

public class Client
{
	/* Preference data */
	public  String         server   = "irc.freenode.net";
	public  int            port     = 6667;
	public  String         nickname = "SpadeGuest";
	public  String         channel  = "#rhnoise";
	public  boolean        usesasl  = false;
	public  String         authname = "";
	public  String         password = "";
	public  String         username = "user";
	public  String         hostname = "localhost";

	/* Public data */
	public  boolean        ready    = false;

       	/* Connection data */
	private Socket         socket;
	private BufferedReader input;
	private PrintWriter    output;

	/* Private data */
	private int            mangle;

	/* Public Methods */
	public Client()
	{
		Os.debug("Client: create");
	}

	public void setServer(String server, int port)
	{
		this.server = server;
		this.port   = port;
	}

	public void setAuth(boolean usesasl, String authname, String password)
	{
		this.usesasl  = usesasl;
		this.authname = authname;
		this.password = password;
	}

	public void setUser(String nickname, String channel)
	{
		this.nickname = nickname;
		this.channel  = channel;
	}

	public boolean connect()
	{
		Os.debug("Client: connect");

		try {
			this.socket = new Socket(this.server, this.port);
			this.input  = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.output = new PrintWriter(this.socket.getOutputStream());
		} catch (Exception e) {
			Os.debug("Client: failed to create connection: " + e);
			return false;
		}

		Os.debug("Client: connected");
		if (this.usesasl)
			this.raw("CAP REQ :sasl");
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
			if (this.usesasl)
				this.dosasl(msg);
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
		if (msg.cmd.equals("433")) {
			this.raw("NICK "+this.nickname+this.mangle);
			this.mangle++;
		}
		if (msg.cmd.equals("PING")) {
			this.raw("PING " + msg.msg);
		}
	}

	private void dosasl(Message msg)
	{
		switch (msg.type) {
			case CAP:
				if (msg.msg.equals("sasl") && msg.arg.equals("ACK")) {
					Os.debug("Client: sasl - starting auth");
					this.raw("AUTHENTICATE PLAIN");
				} else {
					Os.debug("Client: sasl - Server does not support sasl");
				}
				break;
			case AUTH:
				if (msg.arg.equals("+")) {
					Os.debug("Client: sasl - performin authentication");
					this.raw("AUTHENTICATE " + Os.base64(
								this.authname + "\0" +
								this.authname + "\0" +
								this.password));
				} else {
					Os.debug("Client: sasl - unexpected authenticate response");
				}
				break;
			case AUTHOK:
				Os.debug("Client: SASL Auth Successful");
				this.raw("CAP END");
				break;
			case AUTHFAIL:
				Os.debug("Client: SASL Auth Failed");
				this.raw("CAP END");
				break;
		}
	}
}

package org.pileus.spades;

import java.io.*;
import java.net.*;

public class Client
{
	/* Constnats */
	enum State {
		INIT,
		SETUP,
		READY,
		ABORT,
	}

	/* Preference data */
	public  String         server   = "irc.freenode.net";
	public  int            port     = 6667;
	public  int            timeout  = 240;
	public  String         nickname = "SpadeGuest";
	public  String         channel  = "#rhnoise";
	public  boolean        usesasl  = false;
	public  String         authname = "";
	public  String         password = "";
	public  String         username = "user";
	public  String         hostname = "localhost";

	/* Public data */
	public  State          state    = State.INIT;
	public  String         name     = "";

	/* Connection data */
	private boolean        pinging;
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
		this.name     = nickname;
	}

	public boolean connect()
	{
		Os.debug("Client: connect");

		try {
			this.state  = State.SETUP;
			this.socket = new Socket();
			this.socket.setSoTimeout(this.timeout/2 * 1000);
			this.socket.connect(new InetSocketAddress(this.server, this.port));
			this.input  = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.output = new PrintWriter(this.socket.getOutputStream());
		} catch (IOException e) {
			Os.debug("Client: failed to create connection: " + e);
			return false;
		}

		Os.debug("Client: connected");
		if (this.usesasl)
			this.raw("CAP REQ :sasl");
		this.raw("USER "+this.username+" "+this.hostname+" "+this.server+" :"+this.name);
		this.raw("NICK "+this.name);

		return true;
	}

	public void abort()
	{
		Os.debug("Client: abort");
		this.state = State.ABORT;
		this.validate();
	}

	public void reset()
	{
		Os.debug("Client: reset");
		this.state = State.INIT;
	}

	public void raw(String line)
	{
		if (this.validate() != State.SETUP &&
		    this.validate() != State.READY)
			return;
		Os.debug("< " + line);
		this.output.println(line);
		this.output.flush();
	}

	public Message send(String dst, String txt)
	{
		if (txt == null || txt.length() == 0)
			return null;
		if (this.validate() != State.READY)
			return null;
		Message msg = new Message(dst, this.name, txt);
		if (msg.type == Message.Type.JOIN)
			this.channel = msg.msg;
		this.raw(msg.line);
		return msg;
	}

	public Message send(String txt)
	{
		return this.send(this.channel, txt);
	}

	public Message recv()
	{
		while (true) try {
			if (this.validate() != State.SETUP &&
			    this.validate() != State.READY)
				return null;
			String line = this.input.readLine();
			if (line == null)
				return null;
			Os.debug("> " + line);
			Message msg = new Message(line, this.name);
			this.process(msg);
			if (this.usesasl)
				this.dosasl(msg);
			if (!msg.cmd.equals("PING") &&
			    !msg.cmd.equals("PONG"))
				return msg;
		}
		catch (SocketTimeoutException e) {
			if (this.pinging) {
				this.abort();
				return null;
			} else {
				this.pinging = true;
				this.raw("PING :" + hostname);
				continue;
			}
		}
		catch (SocketException e) {
			this.state = State.INIT;
			return null;
		}
		catch (IOException e) {
			this.state = State.INIT;
			Os.debug("Client: error in recv", e);
			return null;
		}
	}

	/* Private methods */
	private State validate()
	{
		try {
			if (this.state == State.ABORT) {
				if (this.socket != null) {
					this.socket.close();
					this.state = State.INIT;
				}
			}
		} catch (IOException e) {
			Os.debug("Client: error closing socket", e);
		}
		return this.state;
	}

	private void process(Message msg)
	{
		if (msg.cmd.equals("001") && msg.msg.matches("Welcome.*")) {
			this.raw("JOIN "  + this.channel);
			this.raw("TOPIC " + this.channel);
			this.state = State.READY;
		}
		if (msg.cmd.equals("433")) {
			this.name   = this.nickname + this.mangle;
			this.mangle = this.mangle + 11;
			this.raw("NICK "  + this.name);
		}
		if (msg.cmd.equals("PING")) {
			this.raw("PONG " + msg.msg);
		}
		if (msg.cmd.equals("PONG")) {
			this.pinging = false;
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

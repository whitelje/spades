package org.pileus.spades;

import java.io.BufferedReader;
import java.io.PrintWriter;

import android.util.Log;

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
			Log.d("Spades", "> " + line);
			return line;
		} catch (Exception e) {
			Log.d("Spades", "Error reading line", e);
			this.running = false;
			return "";
		}
	}

	private void putline(String line)
	{
		try {
			Log.d("Spades", "< " + line);
			output.println(line);
			output.flush();
		} catch (Exception e) {
			Log.d("Spades", "Error writing line", e);
			this.running = false;
		}
	}

	/* Public Methods */
	public Client(String server, String nickname, String channel,
			String username, String hostname)
	{
		this.server   = server;
		this.nickname = nickname;
		this.channel  = channel;
		this.username = username;
		this.hostname = hostname;
		Log.d("Spades", "Client create");
	}

	public Client(String server, String nickname, String channel)
	{
		this(server, nickname, channel, "user", "localhost");
		Log.d("Spades", "Client create");
	}

	public void connect(BufferedReader input, PrintWriter output)
	{
		this.input  = input;
		this.output = output;
		Log.d("Spades", "Client connect");
		putline("USER "+username+" "+hostname+" "+server+" :"+nickname);
		putline("NICK "+nickname);
	}

	public void send(String txt)
	{
	}

	public Message recv()
	{
		try {
			String line = getline();
			Message msg = new Message(line);
			process(msg);
			return msg;
		} catch (Exception e) {
			Log.d("Spades", "Error in recv", e);
			this.running = false;
			return null;
		}
	}
}

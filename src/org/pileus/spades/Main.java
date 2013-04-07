package org.pileus.spades;

import java.io.*;
import java.net.*;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Main extends Activity
{
	/* Configuration */
	private String server   = "irc.freenode.net";
	private String nickname = "andydroid";
	private String channel  = "#rhnoise";
	private int    port     = 6667;

	/* Private data */
	private Socket socket = null;
	private Client client = null;

	/* Public Methods */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			this.socket = new Socket(server, port);
			this.client = new Client(server, nickname, channel);
			Log.d("Spades", "Socket and client created");
		} catch(Exception e) {
			Log.d("Spades", "Failed to create socket: " + e);
			return;
		}

		try {
			BufferedReader input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter    output = new PrintWriter(socket.getOutputStream());
			this.client.connect(input, output);
			Log.d("Spades", "Client connected");
		} catch (Exception e) {
			Log.d("Spades", "Failed to create readers writers: " + e);
			return;
		}

		TextView text = (TextView)findViewById(R.id.textview);
		while (client.running) {
			Message msg = client.recv();
			if (msg == null)
				continue;
		}
	}
}

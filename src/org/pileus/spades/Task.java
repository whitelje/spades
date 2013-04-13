package org.pileus.spades;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.widget.Toast;

public class Task extends Service implements Runnable
{
	/* Commands */
	public static final int REGISTER   = 0;
	public static final int MESSAGE    = 1;
	public static final int CONNECT    = 2;
	public static final int DISCONNECT = 3;

	/* Configuration */
	private String    server    = "irc.freenode.net";
	private String    nickname  = "andydroid";
	private String    channel   = "#rhnoise";

	/* Private data */
	private Messenger messenger = null;
	private Thread    thread    = null;
	private Client    client    = null;
	private Toast     toast     = null;

	/* Private methods */
	private void command(int cmd, Object value)
	{
		try {
			android.os.Message msg = android.os.Message.obtain();
			msg.what = cmd;
			msg.obj  = value;
			this.messenger.send(msg);
		} catch (Exception e) {
			Os.debug("Task: error sending message");
		}
	}

	private void notify(String text, int icon)
	{
		// Log
		Os.debug("Task: notify - " + text);

		// Toast
		this.toast.setText(text);
		this.toast.show();

		// Notify
		Notification  note   = new Notification(icon, null, 0);
		Intent        intent = new Intent(this, Main.class);
		PendingIntent pend   = PendingIntent.getActivity(this, 0, intent, 0);

		note.setLatestEventInfo(this, "Spades!", text, pend);
		this.startForeground(1, note);
	}

	/* Public methods */
	public Message send(String txt)
	{
		if (this.client == null)
			return null;
		Message msg = this.client.send(txt);
		if (msg != null)
			this.command(MESSAGE, msg);
		return msg;
	}

	/* Runnable methods */
	@Override
	public void run()
	{
		Os.debug("Task: thread run");

		// Android Toast setup
		Looper.prepare();

		// Setup notification bar
		this.notify("Connecting..", android.R.drawable.presence_invisible);

		// Start connecting
		if (!this.client.connect(server, nickname, channel)) {
			this.command(DISCONNECT, null);
			this.notify("Unable to connect", android.R.drawable.presence_offline);
			this.thread = null;
			return;
		}

		// Wait for login
		while (!this.client.ready) {
			Message msg = this.client.recv();
			if (msg == null)
				break;
			this.command(MESSAGE, msg);
		}

		// Notify connection status
		if (this.client.ready) {
			this.command(CONNECT, null);
			this.notify("Connected", android.R.drawable.presence_online);
		} else {
			this.command(DISCONNECT, null);
			this.notify("Connetion aborted", android.R.drawable.presence_offline);
		}

		// Process messages
		while (this.client.ready) {
			Message msg = this.client.recv();
			if (msg == null)
				break;
			this.command(MESSAGE, msg);
		}

		// Notify disconnect disconnected
		this.notify("Disconnected", android.R.drawable.presence_offline);
		this.command(DISCONNECT, null);

		// Shutdown the client
		this.client.abort();
		this.thread = null;

		Os.debug("Task: thread exit");
	}

	/* Service Methods */
	@Override
	public void onCreate()
	{
		Os.debug("Task: onCreate");
		super.onCreate();

		// Setup toast
		Context context = this.getApplicationContext();
		this.toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

		// Create the client
		this.client = new Client();
	}
        
	@Override
	public void onDestroy()
	{
		Os.debug("Task: onDestroy");
		try {
			this.client.abort();
			this.thread.join();
		} catch (Exception e) {
			Os.debug("Task: error stopping service", e);
		}
	}
        
	@Override
	public void onStart(Intent intent, int startId)
	{
		Os.debug("Task: onStart");
		super.onStart(intent, startId);

		// Setup communication with Main
		this.messenger = (Messenger)intent.getExtras().get("Messenger");
		this.command(REGISTER, this);

		// Create client thread
		if (this.thread == null) {
			this.thread = new Thread(this);
			this.thread.start();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Os.debug("Task: onBind");
		return null;
	}
}

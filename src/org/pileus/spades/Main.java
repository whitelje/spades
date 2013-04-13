package org.pileus.spades;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;

public class Main extends Activity
{
	/* Static data */
	private Handler      handler; 
	private Messenger    messenger; 

	/* Private data */
	private Task         task;

	/* Widgets */
	private TabHost      window;
	private TabWidget    tabs;
	private LinearLayout chat;
	private TextView     log;
	private EditText     input;
	private Button       send;
	private TextView     spades;
	private TextView     debug;

	private ScrollView   lscroll;
	private ScrollView   dscroll;

	/* Private methods */
	public void onRegister(Object obj)
	{
		Os.debug("Main: onRegister");
		this.task = (Task)obj;
	}

	public void onMessage(Object obj)
	{
		Message msg = (Message)obj;

		this.debug.append("> " + msg.line + "\n");
		this.dscroll.smoothScrollTo(0, this.debug.getBottom());

		if (msg.cmd.equals("PRIVMSG")) {
			this.log.append(msg.from + ": " + msg.msg + "\n");
			this.lscroll.smoothScrollTo(0, this.log.getBottom());
		}
	}

	private void startService()
	{
		Os.debug("Main: startService");
		startService(new Intent(this, Task.class)
				.putExtra("Messenger", this.messenger));
	}

	private void stopService()
	{
		Os.debug("Main: stopService");
		stopService(new Intent(this, Task.class));
	}

	/* Widget callback functions */
	public void onSend(View btn)
	{
		if (this.task == null)
			return;
		String  txt = this.input.getText().toString();
		Message msg = this.task.send(txt);
		if (msg == null)
			return;
		this.input.setText("");
		this.log.append(msg.from + ": " + msg.msg + "\n");
	}

	/* Activity Methods */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		try {
			super.onCreate(savedInstanceState);
			Os.debug("Main: onCreate");

			// Setup main layout
			this.setContentView(R.layout.main);

			// Setup communication
			this.handler   = new MainHandler();
			this.messenger = new Messenger(this.handler);

			// Find widgets
			this.window    = (TabHost)      findViewById(android.R.id.tabhost);
			this.tabs      = (TabWidget)    findViewById(android.R.id.tabs);
			this.chat      = (LinearLayout) findViewById(R.id.chat);
			this.log       = (TextView)     findViewById(R.id.log);
			this.input     = (EditText)     findViewById(R.id.input);
			this.send      = (Button)       findViewById(R.id.send);
			this.spades    = (TextView)     findViewById(R.id.spades);
			this.debug     = (TextView)     findViewById(R.id.debug);

			this.lscroll   = (ScrollView)   findViewById(R.id.log_scroll);
			this.dscroll   = (ScrollView)   findViewById(R.id.debug_scroll);

			// Add window tabs
			this.window.setup();

			this.window.addTab(this.window
					.newTabSpec("chat")
					.setIndicator("Chat")
					.setContent(R.id.chat));
			this.window.addTab(this.window
					.newTabSpec("spades")
					.setIndicator("Spades")
					.setContent(R.id.spades));
			this.window.addTab(this.window
					.newTabSpec("debug")
					.setIndicator("Debug")
					.setContent(R.id.debug));
		} catch (Exception e) {
			Os.debug("Error setting content view", e);
			return;
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		Os.debug("Main: onStart");
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Os.debug("Main: onResume");
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Os.debug("Main: onPause");
	}

	@Override
	public void onStop()
	{
		super.onStop();
		Os.debug("Main: onStop");
	}

	@Override
	public void onRestart()
	{
		super.onRestart();
		Os.debug("Main: onRestart");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Os.debug("Main: onDestroy");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.connect:
				this.startService();
				return true;
			case R.id.disconnect:
				this.stopService();
				return true;
			case R.id.help:
				Os.debug("Main: Help!");
				return true;
			default:
				return false;
		}
	}

	/* Handler class */
	class MainHandler extends Handler
	{
		public void handleMessage(android.os.Message msg)
		{
			switch (msg.what) {
				case Task.REGISTER:
					Main.this.onRegister(msg.obj);
					break;
				case Task.MESSAGE:
					Main.this.onMessage(msg.obj);
					break;
				default:
					Os.debug("Main: unknown message - " + msg.what);
					break;
			}
		}
	}
}


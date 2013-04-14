package org.pileus.spades;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity
{
	/* Private data */
	private Handler      handler;
	private Messenger    messenger;
	private Task         task;
	private Toast        toast;
	private boolean      ready;
	private String       topic;
	private String       names;

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

	/* Private helper methods */
	private void notice(String text)
	{
		this.log.append(Html.fromHtml("<b>*** " + text + "</b><br />"));
	}

	private void display(Message msg)
	{
		String when = DateFormat.format("hh:mm:ss", msg.time).toString();
		String from = String.format("<font color=\"#ff88ff\">%s</font>", msg.from);
		String text = msg.msg;
		String fmt  = null;

		// Do IRC Colors - only partly works
		String fg = "<font color=\"<$1>\">";
		text = text
			.replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;");
		text = text
			.replaceAll("\\002", "<b>")             // bold
			.replaceAll("\\011", "<i>")             // italic
			.replaceAll("\\025", "<u>");            // underline
		text = text
			.replaceAll("\\003(\\d+)(,\\d+)?", fg)  // color
			.replaceAll("\\013(\\d+)(,\\d+)?", fg); // color
		text = text
			.replaceAll("<0?0>", "#000000")         // White
			.replaceAll("<0?1>", "#000000")         // Black
			.replaceAll("<0?2>", "#000080")         // Navy Blue
			.replaceAll("<0?3>", "#008000")         // Green
			.replaceAll("<0?4>", "#FF0000")         // Red
			.replaceAll("<0?5>", "#804040")         // Brown
			.replaceAll("<0?6>", "#8000FF")         // Purple
			.replaceAll("<0?7>", "#808000")         // Olive
			.replaceAll("<0?8>", "#FFFF00")         // Yellow
			.replaceAll("<0?9>", "#00FF00")         // Lime Green
			.replaceAll("<10>",  "#008080")         // Teal
			.replaceAll("<11>",  "#00FFFF")         // Aqua Light
			.replaceAll("<12>",  "#0000FF")         // Royal Blue
			.replaceAll("<13>",  "#FF00FF")         // Hot Pink
			.replaceAll("<14>",  "#808080")         // Dark Gray
			.replaceAll("<15>",  "#C0C0C0");        // Light Gray

		// Message formatting
		switch (msg.how) {
			case DIRECT:
			case MENTION:
			case PRIVMSG:
				fmt  = "<b>(%s) %s: %s</b>";
				break;
			case SENT:
				fmt  = "(%s) <b>%s</b>: %s";
				break;
			default:
				fmt  = "(%s) %s: %s";
				break;
		}

		String html = String.format(fmt, when, from, text);
		this.log.append(Html.fromHtml(html + "<br />"));
	}

	/* Private handler methods */
	private void onRegister(Object obj)
	{
		Os.debug("Main: onRegister");
		this.task = (Task)obj;
	}

	private void onMessage(Object obj)
	{
		Message msg = (Message)obj;

		// Debug
		this.debug.append("> " + msg.line + "\n");
		this.dscroll.smoothScrollTo(0, this.debug.getBottom());

		// Chat
		switch (msg.type) {
			case PRIVMSG:
				this.display(msg);
				break;
			case TOPIC:
				if (!msg.txt.equals(this.topic))
					this.notice("Topic for " + msg.arg + ": " + msg.txt);
				this.topic = msg.txt;
				break;
			case NAMES:
				if (!msg.txt.equals(this.names))
					this.notice("Users in " + msg.arg + ": " + msg.txt);
				this.names = msg.txt;
				break;
			case ERROR:
				this.notice("Error: " + msg.txt);
				break;
			case AUTHOK:
				this.notice("Authentication succeeded: " + msg.txt);
				break;
			case AUTHFAIL:
				this.notice("Authentication failed: " + msg.txt);
				break;
		}
		this.lscroll.smoothScrollTo(0, this.log.getBottom());
	}

	private void onNotify(String text)
	{
		Os.debug("Main: onNotify - " + text);
		this.notice(text);
		this.toast.setText(text);
		this.toast.show();
	}

	/* Private service methods */
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

			// Setup toast
			this.toast     = Toast.makeText(this, "", Toast.LENGTH_SHORT);

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
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.connect).setVisible(!this.ready);
		menu.findItem(R.id.disconnect).setVisible(this.ready);
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
			case R.id.settings:
				this.startActivity(new Intent(this, Prefs.class));
				return true;
			case R.id.exit:
				this.stopService();
				this.finish();
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
				case Task.CONNECT:
					Main.this.ready = true;
					break;
				case Task.DISCONNECT:
					Main.this.ready = false;
					break;
				case Task.NOTIFY:
					Main.this.onNotify((String)msg.obj);
					break;
				default:
					Os.debug("Main: unknown message - " + msg.what);
					break;
			}
		}
	}
}

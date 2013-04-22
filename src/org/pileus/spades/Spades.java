package org.pileus.spades;

public class Spades
{
	/* Private data */
	public Task  task;
	public Cards cards;

	/* Widget callback functions */
	public Spades()
	{
	}

	/* IRC Callbacks */
	public void onMessage(Message msg)
	{
		Os.debug("Spades: onMessage");
	}

	/* UI Callbacks */
	public boolean onBid(int bid)
	{
		Os.debug("Spades: onBid - " + bid);
		return this.send(".bid " + bid);
	}

	public boolean onPass(String card)
	{
		Os.debug("Spades: onPass - " + card);
		return this.send(".pass " + card);
	}

	public boolean onLook()
	{
		Os.debug("Spades: onLook");
		return this.send(".look");
	}

	public boolean onPlay(String card)
	{
		Os.debug("Spades: onPlay - " + card);
		return this.send(".play " + card);
	}

	public boolean onTurn()
	{
		Os.debug("Spades: onTurn");
		return this.send(".turn");
	}

	/* Helper functions */
	private boolean send(String msg)
	{
		if (this.task == null)
			return false;
		this.task.send(msg);
		return true;
	}
}

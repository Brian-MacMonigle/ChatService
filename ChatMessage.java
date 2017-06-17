package chat;

import java.io.Serializable;

public class ChatMessage implements Serializable
{
	/**
	 * Generated by Eclipse 5/31/2017 12:50 pm
	 */
	private static final long serialVersionUID = 3012002052771595388L;

	public static final int MESSAGE = 0, LOGOUT = 1, CONNECT = 2, OLD_CONNECT = 3, CHANGE_NAME = 4;

	int type;
	String message;
	String username;
	String date;

	public ChatMessage(int type, String message, String username, String date)
	{
		this.type = type;
		this.message = message;
		this.username = username;
		this.date = date;
	}

	public int getType()
	{
		return type;
	}

	public String getMessage()
	{
		return message;
	}

	public String getOwner()
	{
		return username;
	}
}

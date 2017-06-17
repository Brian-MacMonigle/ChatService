package chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ChatClient implements ActionListener
{
	public static void main(String[] args)
	{
		JFrame frame = new JFrame("ChatMaster Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ChatClient client = new ChatClient();
		frame.add(client.getFullGui());
		frame.pack();
		frame.setLocation(150, 100);
		frame.setVisible(true);
	}

	private final int INIT_SERVER_LEN = 10, INIT_PORT_LEN = 5;
	private final int INIT_COL = 35, INIT_ROW = 30;
	private final int INIT_USER_LEN = 15, INIT_MSG_LEN = 25;
	private final String INIT_SERVER = "localhost";

	private static final String newline = System.getProperty("line.separator");

	private JPanel fullGui, chatGui;

	// Server tf, Port tf, Username tf, Message tf
	private JTextField stf, ptf, utf, mtf;
	// Connect, Change Name, Send Message, Logout
	private JButton cb, cn, sm, lo;
	// (Chat) ta, User ta
	private JTextArea ta, uta;
	// The username held by the client (NOTE: Seperate from the utf).
	private String username;
	// List of connected users. Given to Client by Server.
	private ArrayList<String> users;

	// The socket connection to the server. Null when not in use
	private Socket socket;
	// I/O
	private ObjectInputStream in;
	private ObjectOutputStream out;

	// Client listener. NOTE: If cl is null then not connected to a server
	private ListenThread cl;

	public ChatClient()
	{
		initGui();
		initClient();
	}

	private void initClient()
	{
		cl = null;
		users = new ArrayList<String>();
	}

	private void initGui()
	{
		fullGui = new JPanel(new BorderLayout());

		chatGui = initChatPane();

		fullGui.add(initConBar(), BorderLayout.PAGE_START);
		fullGui.add(chatGui, BorderLayout.CENTER);
	}

	private JPanel initChatPane()
	{
		JPanel chatPane = new JPanel(new BorderLayout());
		chatPane.add(initUserBar(), BorderLayout.PAGE_START);
		chatPane.add(initChat(), BorderLayout.CENTER);
		return chatPane;
	}

	private JPanel initConBar()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		stf = new JTextField(INIT_SERVER_LEN);
		stf.addActionListener(this);
		stf.setText(INIT_SERVER);
		ptf = new JTextField(INIT_PORT_LEN);
		ptf.addActionListener(this);
		cb = new JButton("Connect");
		cb.addActionListener(this);
		lo = new JButton("Logout");
		lo.addActionListener(this);
		lo.setEnabled(false);

		panel.add(new JLabel("Server Address:  "));
		panel.add(stf);
		panel.add(new JLabel("Port:  "));
		panel.add(ptf);
		panel.add(cb);
		panel.add(lo);

		return panel;
	}

	private JPanel initUserBar()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		// Gives username random number [1,999]
		username = "Anonymous " + (int) (Math.random() * 999 + 1);
		utf = new JTextField(username, INIT_USER_LEN);

		utf.addActionListener(this);
		utf.getDocument().addDocumentListener(new DocumentListener()
		{

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				utfChanged(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				utfChanged(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				utfChanged(e);
			}

			void utfChanged(DocumentEvent e)
			{
				if (utf.getText().equals(username))
				{
					cn.setEnabled(false);
				}
				else
				{
					cn.setEnabled(true);
				}
			}
		});

		cn = new JButton("Change Name");
		cn.addActionListener(this);
		cn.setEnabled(false);

		panel.add(new JLabel("Username:  "));
		panel.add(utf);
		panel.add(cn);

		return panel;
	}

	private JTabbedPane initChat()
	{
		JTabbedPane panel = new JTabbedPane();

		panel.add("Chat", initChatBox());

		uta = new JTextArea(INIT_ROW, INIT_COL);
		uta.setEditable(false);
		panel.addTab("Users", new JScrollPane(uta));

		return panel;
	}

	private JPanel initChatBox()
	{
		JPanel panel = new JPanel(new BorderLayout());

		ta = new JTextArea(INIT_ROW, INIT_COL);
		ta.setEditable(false);

		panel.add(new JScrollPane(ta), BorderLayout.CENTER);
		panel.add(initMessageBar(), BorderLayout.PAGE_END);

		return panel;
	}

	private JPanel initMessageBar()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		mtf = new JTextField(INIT_MSG_LEN);
		mtf.addActionListener(this);
		sm = new JButton("Send");
		sm.addActionListener(this);
		sm.setEnabled(false);

		panel.add(new JLabel("Message:  "));
		panel.add(mtf);
		panel.add(sm);

		return panel;
	}

	// Updates the user gui
	private synchronized void updateUserGui()
	{
		uta.setText("");
		for (String u : users)
		{
			uta.append(u + newline);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object s = e.getSource();
		if (s == cn || s == utf) // Process change name
		{
			setUsername(utf.getText());
		}
		else if (s == mtf || s == sm) // Send message
		{
			write(new ChatMessage(ChatMessage.MESSAGE, mtf.getText(), null, null));
			mtf.setText("");
		}
		else if (s == cb || s == stf || s == ptf) // Connect
		{
			try
			{
				connect(stf.getText(), Integer.parseInt(ptf.getText()));
			}
			catch (IllegalArgumentException ex)
			{
				log("Error with the given port: " + ptf.getText());
			}
		}
		else if (s == lo) // Logout
		{
			logout();
		}
	}

	public JPanel getFullGui()
	{
		return fullGui;
	}

	public JPanel getChatGui()
	{
		return chatGui;
	}

	public boolean connected()
	{
		return cl != null;
	}

	public boolean connect(String address, int port)
	{
		if (connected())
		{
			logout();
		}
		try
		{
			socket = new Socket(address, port);

			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		}
		catch (UnknownHostException e)
		{
			log("Error with the given host: " + stf.getText());
			return false;
		}
		catch (IOException e)
		{
			log("Error connecting to host: " + e.getMessage());
			return false;
		}

		try
		{
			out.writeObject(username);
		}
		catch (IOException e)
		{
			log("Error sending username to host: " + e);
			return false;
		}

		cl = new ListenThread();

		cl.start();

		// Update gui
		log("Connected to server.");
		stf.setText(address);
		ptf.setText("" + port);
		sm.setEnabled(true);
		lo.setEnabled(true);
		addUser(username);
		return true;
	}

	public void logout()
	{
		// Tell server Client is logging out
		write(new ChatMessage(ChatMessage.LOGOUT, null, null, null));
		// Cleanup
		closeConnection();

		// Update gui
		log("Logging off.");
		users.clear();
	}

	private void handleDisconnect()
	{
		closeConnection();
		log("Lost connection to server.");
		clearUsers();
	}

	private boolean write(ChatMessage msg)
	{
		try
		{
			out.writeObject(msg);
			return true;
		}
		catch (IOException e)
		{
			log("Error sending message: " + e);
			e.printStackTrace();
			return false;
		}
	}

	private void log(String msg)
	{
		ta.append(msg + newline);
		ta.moveCaretPosition(ta.getText().length());
	}

	private void closeConnection()
	{
		if (cl != null)
		{
			cl.closeListener();
		}
		if (in != null)
		{
			try
			{
				in.close();
			}
			catch (IOException e)
			{} // Not much I can do
		}
		if (out != null)
		{
			try
			{
				out.close();
			}
			catch (IOException e)
			{} // Not much I can do
		}
		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch (IOException e)
			{} // Not much I can do
		}

		users.clear();
		sm.setEnabled(false);
		lo.setEnabled(false);
		cl = null;
		socket = null;
		out = null;
		in = null;
	}

	public void setUsername(String newName)
	{
		username = newName;

		// Tell server that I changed my name
		if (connected())
		{
			write(new ChatMessage(ChatMessage.CHANGE_NAME, newName, null, null));
		}

		// Update gui
		cn.setEnabled(false);
		utf.setText(username);
	}

	private synchronized void addUser(String usr)
	{
		users.add(usr);
		updateUserGui();
	}

	private synchronized void removeUser(String usr)
	{
		users.remove(usr);
		updateUserGui();
	}

	private synchronized void clearUsers()
	{
		users.clear();
		updateUserGui();
	}

	private void readMessage(ChatMessage msg)
	{
		if (ChatMessage.MESSAGE == msg.getType())
		{
			log(msg.getOwner() + ": " + msg.getMessage());
		}
		else if (ChatMessage.LOGOUT == msg.getType())
		{
			log(msg.getOwner() + " has logged out.");
			removeUser(msg.getOwner());
		}
		else if (ChatMessage.CONNECT == msg.getType())
		{
			log(msg.getOwner() + " has connected.");
			addUser(msg.getOwner());
		}
		else if (ChatMessage.CHANGE_NAME == msg.getType())
		{
			log(msg.getOwner() + " has changed username to '" + msg.getMessage() + "'.");
			removeUser(msg.getOwner());
			addUser(msg.getMessage());
		}
		else
		{
			log("Recieved unknown message type from server.");
		}
	}

	private class ListenThread extends Thread
	{
		// Keep listening to server
		boolean keepGoing;

		public ListenThread()
		{
			keepGoing = true;
		}

		@Override
		public void run()
		{
			while (keepGoing)
			{
				try
				{
					// Run message through interpreter
					readMessage((ChatMessage) in.readObject());
				}
				// Could not readobjectsent
				catch (ClassNotFoundException | ClassCastException e)
				{
					log("Message unreadable: " + e);
				}
				catch (IOException e) // Lost connection to server
				{
					if (keepGoing)
					{
						handleDisconnect();
					}
				}
			}
		}

		// Used by close function
		void closeListener()
		{
			keepGoing = false;
		}
	}

}

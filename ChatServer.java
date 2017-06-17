package chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatServer implements ActionListener
{
	public static void main(String[] args)
	{
		JFrame frame = new JFrame("ChatMaster Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ChatServer server = new ChatServer();
		frame.add(server.getFullGui());
		frame.pack();
		frame.setLocation(150, 100);
		frame.setVisible(true);
	}

	private static final int INIT_SERVER_LEN = 10, INIT_PORT_LEN = 5;
	private static final int INIT_COL = 35, INIT_ROW = 30;
	private static final int INIT_MSG_LEN = 25;

	private static final String newline = System.getProperty("line.separator");

	private JPanel fullGui;

	// Input fields for server and port
	private JTextField stf, ptf;
	// Button attempts to host and shutdown server
	private JButton hb, sd;

	// The message input field
	private JTextField mtf;
	// The Chat text box and Userlist text box and Log text box
	private JTextArea ta, uta, lta;
	// Button to send message, debug clear button
	private JButton sb, dcb;
	// Holds the loop running on server
	private Thread sl;
	// The server
	private ServerSocket server;
	// Listen for incomming connections
	private boolean keepGoing;
	// List of connected Clients
	private ArrayList<ClientThread> cal;
	// Turn dates into strings
	private SimpleDateFormat sdf;

	public ChatServer()
	{
		initGui();
		initServer();
	}

	private void initGui()
	{
		fullGui = new JPanel(new BorderLayout());

		fullGui.add(initConBar(), BorderLayout.PAGE_START);
		fullGui.add(initChat(), BorderLayout.CENTER);
	}

	private JPanel initConBar()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		stf = new JTextField("localhost", INIT_SERVER_LEN);
		try
		{
			stf.setText("" + InetAddress.getLocalHost().getHostAddress());
		}
		// Not much I can do here. (Probably not
		// connected to internet)
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}

		stf.setEditable(false);
		// stf.setBackground(Color.WHITE);
		ptf = new JTextField(INIT_PORT_LEN);
		ptf.addActionListener(this);
		hb = new JButton("Host");
		hb.addActionListener(this);
		sd = new JButton("Shutdown");
		sd.addActionListener(this);
		sd.setEnabled(false);

		panel.add(new JLabel("Server Address:  "));
		panel.add(stf);
		panel.add(new JLabel("Port:  "));
		panel.add(ptf);
		panel.add(hb);
		panel.add(sd);

		return panel;
	}

	private JTabbedPane initChat()
	{
		JTabbedPane panel = new JTabbedPane();

		panel.addTab("Chat", initChatPanel());
		panel.addTab("Users", initUserPanel());
		panel.addTab("Debug Log", initDebugLog());

		return panel;
	}

	private JPanel initChatPanel()
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
		sb = new JButton("Send");
		sb.addActionListener(this);

		panel.add(new JLabel("Message:  "));
		panel.add(mtf);
		panel.add(sb);

		return panel;
	}

	private JScrollPane initUserPanel()
	{
		uta = new JTextArea(INIT_ROW, INIT_COL);
		uta.setEditable(false);
		return new JScrollPane(uta);
	}

	private JPanel initDebugLog()
	{
		JPanel panel = new JPanel(new BorderLayout());
		lta = new JTextArea(INIT_ROW, INIT_COL);
		lta.setEditable(false);

		panel.add(new JScrollPane(lta), BorderLayout.CENTER);
		panel.add(initDebugClearBar(), BorderLayout.PAGE_END);

		return panel;
	}

	private JPanel initDebugClearBar()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		dcb = new JButton("Clear");
		dcb.addActionListener(this);

		panel.add(dcb);

		return panel;
	}

	public JPanel getFullGui()
	{
		return fullGui;
	}

	private void initServer()
	{
		cal = new ArrayList<ClientThread>();
		sl = null;
		server = null;
		sdf = new SimpleDateFormat("HH:mm:ss");
	}

	// Updates the user gui
	private synchronized void updateUserGui()
	{
		uta.setText("");
		for (ClientThread c : cal)
		{
			uta.append(c.username + newline);
		}
	}

	// Starts server on port. If port is 0 then starts on any free port.
	public void start(int port)
	{
		shutdown();
		sl = new Thread()
		{
			@Override
			public void run()
			{
				log("Attempting to start Server...");
				try
				{
					// Starts actual server
					server = new ServerSocket(port);

					// Make gui look nice
					ptf.setText("" + server.getLocalPort());
					log("Server started.");
					broadcast(new ChatMessage(ChatMessage.MESSAGE, "Server started.", "Server Admin",
							sdf.format(new Date())));
					sd.setEnabled(true);

					// Wait for connections
					keepGoing = true;
					while (keepGoing)
					{
						try
						{
							Socket socket = server.accept();
							if (keepGoing)
							{
								startClient(socket);
							}
						}
						catch (IOException e)
						{
							if (keepGoing)
							{
								log("Client connection failed.");
							}
						}
					}
					// Dont need to clean up because when keepGoing is set to
					// false everything is cleaned up
				}
				catch (IOException e) // Error opening ServerSocket
				{
					log("Error starting server: " + e);
				}
				catch (IllegalArgumentException e) // Port too big
				{
					log("Error with given port: '" + port + "' is too big.");
				}
				catch (SecurityException e)
				{
					log("Error with security: " + e);
				}
			}
		};
		sl.start();
	}

	// Seperated because interacting with
	private synchronized void startClient(Socket s)
	{
		ClientThread c = new ClientThread(s);
		if (c.setup())
		{
			c.start(); // incudes broadcast
			// Send all currently connected users
			for (ClientThread cl : cal)
			{
				c.write(new ChatMessage(ChatMessage.CONNECT, null, cl.username, sdf.format(new Date())));
			}
			cal.add(c);
			updateUserGui();
		}
	}

	private boolean isUsernameAvalible(String user)
	{
		for (ClientThread c : cal)
		{
			if (c.username.equalsIgnoreCase(user))
			{
				return false;
			}
		}
		return true;
	}

	public boolean running()
	{
		return server != null;
	}

	public String getAddress()
	{
		if (server != null)
		{
			return server.getInetAddress().getHostAddress();
		}
		return null;
	}

	public int getPort()
	{
		if (server != null)
		{
			return server.getLocalPort();
		}
		return -1;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object s = e.getSource();
		if (s == hb || s == ptf)
		{
			try // Retrives port from ptf. If ptf is empty (or 0) start using a
				// random port and set ptf to that port
			{
				String pStr = ptf.getText();
				int port = 0;
				if (pStr.length() > 0)
				{
					port = Integer.parseInt(ptf.getText());
				}
				start(port);
			}
			catch (NumberFormatException ex)
			{
				log("Error with given port: '" + ptf.getText() + "' is not a valid number.");
			}
		}
		else if (s == sd) // close server
		{
			shutdown();
		}
		else if (s == sb || s == mtf)
		{
			if (mtf.getText().length() > 0)
			{
				broadcast(new ChatMessage(ChatMessage.MESSAGE, mtf.getText(), "Server Admin", sdf.format(new Date())));
				mtf.setText("");
			}
		}
		else if (s == dcb)
		{
			lta.setText("");
		}
	}

	private synchronized void broadcast(ChatMessage msg)
	{
		// update gui
		if (ChatMessage.MESSAGE == msg.getType())
		{
			// log("Message broadcasted: " + msg);
			ta.append(msg.getOwner() + ": " + msg.getMessage() + newline);
			ta.setCaretPosition(ta.getText().length());
		}
		else if (ChatMessage.CONNECT == msg.getType())
		{
			ta.append(msg.getOwner() + " has connected." + newline);
			ta.setCaretPosition(ta.getText().length());
		}

		// Send message to all clients
		// Go backwards to alow easy removal from list for disconnected users
		for (int i = cal.size() - 1; i >= 0; i--)
		{
			ClientThread c = cal.get(i);
			// If c is not connected OR write fails remove c
			if (!c.isConnected() || !c.write(msg))
			{
				closeClient(c);
			}
		}
	}

	private synchronized void log(String msg)
	{
		lta.append(msg + newline);
		lta.setCaretPosition(lta.getText().length());
	}

	public void shutdown()
	{
		if (running())
		{
			log("Server is shutting down...");
			broadcast(new ChatMessage(ChatMessage.MESSAGE, "Server has been shutdown.", "Server Admin",
					sdf.format(new Date())));

			// stops server from trying to accept new connections
			closeServer();
		}
	}

	private synchronized void closeClient(ClientThread c)
	{
		c.closeConnection();
		cal.remove(c);
		updateUserGui();
	}

	private void closeServer()
	{
		keepGoing = false;
		// Close client connections
		// This is done in rever order because closeConnection() removes the
		// ClientThread from the list
		for (int i = cal.size() - 1; i >= 0; i--)
		{
			cal.get(i).closeConnection();
		}
		cal.clear();
		// Kill server
		try
		{
			server.close();
		}
		catch (IOException e) // Fatal error, kill program
		{
			JOptionPane.showMessageDialog(null, "Fatal Error: Server couldn't shut down: " + e);
			System.exit(0);
		}

		sd.setEnabled(false);
		sl = null;
		server = null;
		log("Server shutdown.");
	}

	private class ClientThread extends Thread
	{
		// The Socket in which the connection is based upon
		Socket socket;
		// IO
		ObjectInputStream in;
		ObjectOutputStream out;
		// The username of the Client
		String username;
		// Allows listen loop to end
		boolean keepGoing;

		public ClientThread(Socket s)
		{
			socket = s;
			keepGoing = false;
		}

		public boolean setup()
		{
			try
			{
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());

				username = (String) in.readObject();
				if (!isUsernameAvalible(username))
				{
					out.writeObject(new ChatMessage(ChatMessage.CHANGE_NAME, null, null, sdf.format(new Date())));
				}
				keepGoing = true;
				log(username + " has connected");
				broadcast(new ChatMessage(ChatMessage.CONNECT, null, username, sdf.format(new Date())));
				return true;
			}
			catch (IOException e)
			{
				log("Error creating IO streams: " + e);
				closeConnection();
				return false;
			}
			catch (ClassNotFoundException e)
			{
				log("Error reading username from Client " + e);
				closeConnection();
				return false;
			}
			catch (NullPointerException e)
			{
				log("Client disconnected before giving username.");
				closeConnection();
				return false;
			}
		}

		private void readMessage(ClientThread owner, ChatMessage msg)
		{
			if (ChatMessage.MESSAGE == msg.getType())
			{
				broadcast(new ChatMessage(ChatMessage.MESSAGE, msg.getMessage(), username, sdf.format(new Date())));
			}
			else if (ChatMessage.LOGOUT == msg.getType())
			{
				handleLogout();
			}
			else if (ChatMessage.CONNECT == msg.getType())
			{
				// Not quite sure what to do
				log("Recieved connect message from " + username + ".  Not sure what to do.");
			}
			else if (ChatMessage.CHANGE_NAME == msg.getType())
			{
				log(owner.username + " changed name to '" + msg.getMessage() + "'.");
				broadcast(new ChatMessage(ChatMessage.CHANGE_NAME, msg.getMessage(), owner.username,
						sdf.format(new Date())));
				owner.username = msg.getMessage();
				updateUserGui();
			}
			else
			{
				log("Recived unidentified message from client.");
				// Tell Client this operation is not supported.
				owner.write(new ChatMessage(ChatMessage.MESSAGE, "This operation is not supported by this server.",
						"Server Admin", sdf.format(new Date())));
			}
		}

		public void run()
		{
			while (keepGoing)
			{
				try
				{
					readMessage(this, (ChatMessage) in.readObject());
				}
				// If client sends bad message kill client
				catch (ClassNotFoundException | ClassCastException e)
				{
					log("Error reading object type from Client " + username + ".");
					handleDisconnect();
				}
				// Catches IO being closed while listening
				catch (NullPointerException e)
				{}
				// Catches lost connection to Client
				catch (IOException e)
				{
					if (keepGoing)
					{
						handleDisconnect();
					}
				}
			}
		}

		public boolean write(ChatMessage msg)
		{
			try
			{
				out.writeObject(msg);
				return true;
			}
			catch (IOException e)
			{
				log("Error writing '" + msg + "' to Client " + username + ": " + e);
				return false;
			}
		}

		public void closeConnection()
		{
			keepGoing = false;
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{} // Not much I can do
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

			if (socket != null)
			{
				try
				{
					socket.close();
				}
				catch (IOException e)
				{} // Not much I can do
			}
		}

		public boolean isConnected()
		{
			return keepGoing;
		}

		// Logout request from the Client
		private void handleLogout()
		{
			closeClient(this);
			String msg = username + " has logged out.";
			log(msg);
			broadcast(new ChatMessage(ChatMessage.LOGOUT, null, username, sdf.format(new Date())));
		}

		// Lost connection to Client
		private void handleDisconnect()
		{
			closeClient(this);
			log(username + " has disconnected.");
			broadcast(new ChatMessage(ChatMessage.LOGOUT, null, username, sdf.format(new Date())));
		}
	}
}

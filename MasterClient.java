package chat;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class MasterClient
{
	public static void main(String[] args)
	{
		JFrame frame = new JFrame("ChatMaster MasterClient");
		JTabbedPane tp = new JTabbedPane();

		ChatServer server = new ChatServer();
		ChatClient client = new ChatClient();

		tp.addTab("Server", server.getFullGui());
		tp.addTab("Client", client.getFullGui());

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(tp);
		frame.pack();
		frame.setLocation(150, 100);
		frame.setVisible(true);
	}
}

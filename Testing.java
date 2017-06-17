package chat;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class Testing
{
	public static void main(String[] args)
	{
		ChatServer server = new ChatServer();
		server.start(0);

		JFrame sf = new JFrame("Server");
		sf.setLocation(100, 50);
		sf.add(server.getFullGui());
		sf.pack();
		sf.setVisible(true);
		sf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JFrame cf = new JFrame("Client");
		cf.setLocation(600, 50);
		JTabbedPane tp = new JTabbedPane();
		cf.add(tp);
		cf.setVisible(true);
		cf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		for (int i = 0; i < 10; i++)
		{
			ChatClient client = new ChatClient();
			client.setUsername("" + i);
			client.connect(server.getAddress(), server.getPort());
			tp.addTab("" + i, client.getFullGui());
		}
		cf.pack();
	}
}

package caps.client.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

import caps.client.CClient;

public class ClientWindow extends JFrame {
	CClient parent;
	public JLabel ipport = new JLabel();
	public ChatPanel chat;
	public InfoPanel info;
	
	public ClientWindow(CClient parent) {
		super();
		
		this.parent = parent;
		this.chat = new ChatPanel(parent);
		this.info = new InfoPanel(parent);
		
		prepare();
		display();
	}
	
	public void prepare() {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(new BorderLayout());
		ipport.setText("IP/port: " + parent.socket.getInetAddress().toString() + ":" + parent.socket.getPort());
		this.add(ipport, BorderLayout.NORTH);
		this.add(chat, BorderLayout.WEST);
		this.add(info, BorderLayout.EAST);
	}
	
	public void display() {
		this.setTitle("Hearts Client v1.0");
		this.setSize(900, 600);
		this.setResizable(false);
		this.setVisible(true);
	}
}

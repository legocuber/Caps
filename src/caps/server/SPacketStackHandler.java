package caps.server;

import java.io.IOException;

import caps.Packet;

public class SPacketStackHandler extends Thread {
	private SClient parent;
	private PacketStack<Packet> stack;
	public SPacketStackHandler(SClient c) {
		parent = c;
		stack = new PacketStack<Packet>();
		this.setName("Packet Stack Handler");
		this.setDaemon(true);
		this.start();
	}
	
	public void send(byte packetType, byte[]... content) {
		Packet pack = new Packet(packetType, Packet.ccat(content));
		stack.add(pack);
	}
	
	public void run() {
		while (parent.alive) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// this ensures that the while loop is run each time
				e1.printStackTrace();
			}
			if (stack.size() > 0) {
				try {
					stack.pop().send(parent.out);
				} catch (IOException e) {
					e.printStackTrace();
					parent.parentServer.window.chat.println("Error with packet sending: " + e.getMessage());
					parent.dispose();
				}
			}
		}
	}
}

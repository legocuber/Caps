package caps.server;

import java.io.IOException;
import java.net.SocketException;

import caps.Constants;
import caps.Packet;

public class ServerKeepalives extends Thread {
	Server parent = null;
	public ServerKeepalives(Server parent) {
		this.parent = parent;
	}
	public void run() {
		while (parent.status != parent.STATUS_CLOSEGAME) {
			for (SClient c : parent.clients) { 
				try {
					Packet.sendPacket(c.out, Constants.pc_keepalive, new byte[] {});
				} catch (SocketException e) {
					parent.clients.remove(c);
					c.alive = false;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.err.println("Unable to wait during keepalive sending: " + e.getMessage());
			}
		}
	}
}

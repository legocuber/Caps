package caps.server;

import static caps.Constants.*;

import caps.Packet;

public class SClientHandler extends Thread {
	SClient parentClient;
	Server parentServer;
	
	public SClientHandler(SClient parent) {
		this.parentClient = parent;
		this.parentServer = parentClient.parentServer;
	}
	public void run() {
		while (parentClient.alive) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while (parentClient.packets.size() > 0) {
				Packet p = parentClient.packets.remove(0);
				byte packetType = p.packetType;
				byte[] payload = p.payload;
				switch(packetType) {
				case pc_keepalive:
					break;
				case pc_message:
					String msg = new String(payload);
					parentServer.broadcast(pc_message, (parentClient.name + ":" + msg).getBytes());
					break;
				case pc_playerinfo:
					parentClient.name = new String(Packet.shiftBackwards(payload, 8));
					parentServer.broadcast(pc_playerinfo, Packet.longToBytes(parentClient.id), parentClient.name.getBytes());
					break;
				case pc_cardtoplay:
					byte card = (byte) Packet.bytesToLong(payload);
					parentClient.playcard = card;
					break;
				case pc_playerpass:
					parentClient.playcard = card_pass;
					break;
				case pc_completion:
					parentServer.completion(parentClient);
					break;
				case pc_auto:
					card = (byte) Packet.bytesToLong(payload);
					parentServer.autoCompletion(parentClient, card);
					break;
				case pc_multiple:
					byte mult = (byte) Packet.bytesToLong(payload);
					parentClient.playcount = mult;
					break;
				default:
					break;
				}
			}
		}
	}
}

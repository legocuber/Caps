package caps.server;

import static caps.Constants.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import caps.Deck;
import caps.Packet;

public class SClient {
	Socket socket;
	DataInputStream in;
	DataOutputStream out;
	
	Server parentServer;
	CopyOnWriteArrayList<Packet> packets = new CopyOnWriteArrayList<Packet>();
	
	Thread packetReceiver = null;
	Thread packetHandler = null;
	SPacketStackHandler outpacks = null;
	
	boolean alive = true;
	String name = null;
	long id;
	byte playcard = -1;
	Deck currentCards;
	int points = 0;
	int playcount = 1;
	
	public void dispose() {
		parentServer.clients.remove(this);
		this.alive = false;
		parentServer.broadcast(pc_playerdc, Packet.longToBytes(id));
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean ready() {
		boolean hasName = name != null;
		return hasName;
	}
	
	/**
	 * 
	 * @param card The card to beat
	 * @param multiple How many cards (doubles, triples)
	 * @return <code>true</code> if the player has a higher or equal card to <code>card</code> or has a bomb, <code>false</code> otherwise.
	 */
	public boolean canPlay(byte card, int multiple) {
		byte[] counts = new byte[13];
		for (byte c : currentCards.cards) {
			counts[c/4] += 1;
		}
		return counts[0] > 0 || counts[card/4] >= multiple;
	}
	
	/**
	 * 
	 * @param mystart boolean: if it is player's turn
	 * @return boolean: if the player scums out (has more 2's than other cards)
	 */
	public boolean scumOut(boolean mystart) {
		int bombs = 0;
		int cards = currentCards.ncards;
		for (byte c : currentCards.cards) {
			if (c / 4 == 0) {
				bombs += 1;
			}
		}
		
		if (mystart) {
			return bombs >= cards; // if the player needs to start the play, they end up with more bombs
		} else {
			return bombs > cards;
		}
	}
	
	public byte getCardindex(byte topcard, int multiple, boolean force) throws InterruptedException { // asks the client for which card they play. if they don't respond in time, they are autoplayed.
		if (!canPlay(topcard, multiple)) {
			return card_pass;
		}
		playcard = -1;
		outpacks.send(pc_datarequest, new byte[] {pc_cardtoplay, topcard});
		// request which card it is
		int elapsed = 0;
		
		long warning = 10000;
		long total = 20000;
		long until = System.currentTimeMillis() + total; // gives user 20s to play
		
		while (playcard == -1 || (force && playcard == card_pass)) {
			Thread.sleep(100);
			elapsed += 100;
			
			if (elapsed < total && elapsed > (total - warning) && elapsed % 1000 == 0) {
				 outpacks.send(pc_timewarning, Packet.longToBytes(until));
			} else if (elapsed >= total) {
				return 0;
			}
		}
		
		byte tmp = playcard;
		playcard = -1;
		return tmp;
		
	}
	
	public byte getMultiple() throws InterruptedException {
		outpacks.send(pc_datarequest, new byte[] {pc_multiple});
		// request which card it is
		int elapsed = 0;
		
		long warning = 10000;
		long total = 20000;
		long until = System.currentTimeMillis() + total; // gives user 20s to play
		
		while (playcount != -1) {
			Thread.sleep(100);
			elapsed += 100;
			
			if (elapsed < total && elapsed > (total - warning) && elapsed % 1000 == 0) {
				 outpacks.send(pc_timewarning, Packet.longToBytes(until));
			} else if (elapsed >= total) {
				return 1;
			}
		}
		
		byte tmp = (byte) playcount;
		playcount = -1;
		return tmp;
	}
	
	public SClient(Socket sock, Server parent) {
		
		try {
			socket = sock;
			socket.setSoTimeout(5000);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.id = System.currentTimeMillis() + 1;
		this.parentServer = parent;
		this.outpacks = new SPacketStackHandler(this);
		
		packetReceiver = new SClientReceiver(this);
		packetHandler = new SClientHandler(this);
		
		packetReceiver.setName("Packet receiver");
		packetHandler.setName("Packet handler");
		
		packetHandler.setDaemon(true);
		packetReceiver.setDaemon(true);
		
		packetReceiver.start();
		packetHandler.start();
		
		outpacks.send(pc_datarequest, new byte[] {pc_playerinfo});
		outpacks.send(pc_yourid, Packet.longToBytes(this.id));
		//Packet.sendDataPacket(out, pc_currentcards, new Deck().tcpBytes());
		for (SClient from : parent.clients) {
			outpacks.send(pc_playerinfo, Packet.longToBytes(from.id), from.name.getBytes());
		}
	}
}

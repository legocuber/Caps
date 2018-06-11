package caps.client;

import static caps.Constants.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import caps.Deck;
import caps.Packet;
import caps.client.swing.ClientWindow;
public class CClient extends Thread {
	public Socket socket;
	public DataInputStream in;
	public DataOutputStream out;
	public Thread packetHandler;
	public long userid;
	public boolean running = true;
	public boolean gamestarted = false;
	public CopyOnWriteArrayList<byte[]> receivedPackets = new CopyOnWriteArrayList<byte[]>();
	public HashMap<Long, OtherPlayer> otherClients = new HashMap<Long, OtherPlayer>();
	public String username = "Player" + (new Random()).nextInt(100000);
	public ClientWindow window;
	public Deck currentdeck;
	public Deck cardstack;
	public CPacketStackHandler outpacks;
	
	public CClient(Socket sock) {
		try {
			socket = sock;
			socket.setSoTimeout(5000);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		packetHandler = new Thread() {
			public void run() {
				while (running) {
					try {
						int packetLen = in.readInt();
						byte[] packet = new byte[packetLen];
						in.read(packet);
						receivedPackets.add(packet);
					} catch (SocketException e) {
						println("Exiting.." + e.getMessage());
						running = false;
						return;
					} catch (IOException e) {
						e.printStackTrace();
						println("Exiting.." + e.getMessage());
						running = false;
						return;
					}
				}
			}
		};
		this.window = new ClientWindow(this);
		this.outpacks = new CPacketStackHandler(this);
		this.packetHandler.start();
		this.start();
		
		
		
	}
	
	public void run() {
		while (running) {
			try {
				if (receivedPackets.size() == 0) continue;
				byte[] rawPacket = receivedPackets.remove(0);
				byte packetType = rawPacket[0];
				byte[] packetContent = Packet.shiftBackwards(rawPacket);
				switch(packetType) {
				case pc_keepalive:
					outpacks.send(pc_keepalive, new byte[] {});
					break;
				case pc_message:
					String msg = new String(packetContent);
					println(msg);
					break;
				case pc_datarequest:
					switch(packetContent[0]) {
					case pc_playerinfo:
						outpacks.send(pc_playerinfo, Packet.longToBytes(userid), username.getBytes());
						break;
					case pc_cardtoplay:
						println("Requesting your card to play.");
						break;
					case pc_multiple:
						println("Requesting how many cards to play.");
						break;
					}
					break;
				case pc_yourid:
					userid = Packet.bytesToLong(packetContent);
					break;
				case pc_cardstack:
					cardstack = Deck.parseTCP(packetContent);
					this.window.info.setCards(this.window.info.stack, cardstack);
					break;
				case pc_yourcards:
					currentdeck = Deck.parseTCP(packetContent);
					this.window.info.setCards(this.window.info.mycards, currentdeck);
					break;
				case pc_playerinfo:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = new String(Packet.shiftBackwards(packetContent, 8));
						if (!otherClients.containsKey(id)) {
							OtherPlayer player = new OtherPlayer(id);
							player.setName(username);
							
							otherClients.put(id, player);
							if (id != userid) {
								println("Added player: " + username);
							}
						} else {
							this.username = username;
						}
					}
					break;
				case pc_bomb:
					{
						println(nameByPacket(packetContent) + " played a BOMB. The pile is cleared.");
					}
					break;
				case pc_playerturn:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + "'s turn.");
						} else {
							println("YOUR turn.");
						}
					}
					break;
				case pc_scumout:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + " scummed out.");
						} else {
							println("You scummed out.");
						}
					}
					break;
				case pc_playerpass:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + " passes.");
						} else {
							println("You pass.");
						}
					}
					break;
				case pc_playable:
					{
						Deck d = Deck.parseTCP(packetContent);
						this.window.info.setCards(this.window.info.playable, d);
					}
					break;
				case pc_completion:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + " has completed. ");
						} else {
							println("You have completed. ");
						}
					}
					break;
				case pc_auto:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + " has autocompleted. The deck is cleared.");
						} else {
							println("You have autocompleted. The deck is cleared.");
						}
					}
					break;
				case pc_skip:
					{
						long id = Packet.bytesToLong(packetContent);
						String username = nameById(id);
						if (id != userid) {
							println(username + " has skipped the next person.");
						} else {
							println("You have skipped the next person.");
						}
					}
					break;
				case pc_timewarning:
					{
						long time = Packet.bytesToLong(packetContent);
						println("Warning, only " + (time - System.currentTimeMillis()) + "s left.");
					}
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public String nameById(long id) {
		return otherClients.get(id).getName();
	}
	
	public String nameByPacket(byte[] packet) {
		return nameById(Packet.bytesToLong(packet));
	}
	
	public void println(String msg) {
		this.window.chat.println(msg);
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		String ip = "0.0.0.0";
		if (args.length > 0) {
			ip = args[0];
		}
		Socket sock = new Socket(InetAddress.getByName(ip), 5555);
		CClient client = new CClient(sock);
		client.getName();
		
		System.setErr(new PrintStream(new FileOutputStream("client.log")));
	}
}

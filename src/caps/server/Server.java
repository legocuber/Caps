package caps.server;

import static caps.Constants.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.concurrent.CopyOnWriteArrayList;

import caps.Deck;
import caps.Packet;
import caps.server.swing.ServerWindow;

public class Server extends Thread {
	protected CopyOnWriteArrayList<SClient> clients = new CopyOnWriteArrayList<SClient>();
	public ServerSocket serverSocket;
	protected int status = -1;
	protected int port = 5555;
	protected String ip = "127.0.0.1";
	protected Thread listenerThread;
	protected Thread keepaliveThread;
	protected int maxplayers = 2;
	public final int STATUS_ACCEPTING = 0;
	public final int STATUS_INGAME = 1;
	public final int STATUS_CLOSEGAME = 2;
	public ServerWindow window = null;
	public Deck cards = new Deck();
	public SClient completionClient = null;
	
	protected void addClient(SClient c) {
		clients.add(c);
	}
	
	public Server(int port) {
		try {
			this.port = port;
			this.serverSocket = new ServerSocket(this.port);
			this.listenerThread = new ClientListener(this);
			this.ip = serverSocket.getInetAddress().toString();
			this.keepaliveThread = new ServerKeepalives(this);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		this.window = new ServerWindow(this);
	}
	
	public void startServer() {
		this.status = STATUS_ACCEPTING;
		this.listenerThread.start();
		this.keepaliveThread.start();
		this.setName("Server");
		this.start();
	}
	
	public boolean allClientsReady() {
		for (SClient c : clients) {
			if (!c.ready()) {
				return false;
			}
		}
		return true;
	}
	
	public void run() {
		try {
			this.listenerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		broadcast(pc_gamestatus, new byte[] {pc_gamestart});
		// telling clients that game has started
		
		while (!allClientsReady()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		// telling clients about other players. redo at start of game.
		for (SClient to : clients) {
			for (SClient from : clients) {
				to.outpacks.send(pc_playerinfo, Packet.longToBytes(from.id), from.name.getBytes());
			}
		}
		
		for (int i = 0; i < 4; i++) {
			broadcast(pc_message, ("This is the start of a new round. ").getBytes());
			playRound();
			broadcast(pc_message, " --- CURRENT STANDINGS --- \nPoints | Name".getBytes());
			for (SClient s : clients) {
				broadcast(pc_message, (padRight(s.points + "", 6) + "      " + s.name).getBytes());
			}
		}
		
		broadcast(pc_message, " --- FINAL STANDINGS --- \nPoints | Name ".getBytes());
		for (SClient s : clients) {
			broadcast(pc_message, (padRight(s.points + "", 6) + "      " + s.name).getBytes());
		}
		
		broadcast(pc_message, ("[Server] Game has ended. Thank you for playing.").getBytes());
	}
	public void playRound() {
		Deck[] dealt = Deck.shuffled().deal(clients.size());
		for (int i = 0; i < clients.size(); i++) {
			clients.get(i).currentCards = dealt[i].sorted();
			clients.get(i).outpacks.send(pc_yourcards, dealt[i].tcpBytes());
		}
		
		try {
			@SuppressWarnings("unchecked")
			CopyOnWriteArrayList<SClient> ingame = (CopyOnWriteArrayList<SClient>) clients.clone();
			CopyOnWriteArrayList<SClient> order = new CopyOnWriteArrayList<SClient>();
			CopyOnWriteArrayList<SClient> scumout = new CopyOnWriteArrayList<SClient>();
			int index = 0;
			int lastplay = -1;
			final int NULL = 0;
			while (ingame.size() > 1) {
				cards = new Deck(new byte[0]);
				lastplay = -1;
				broadcast(pc_cardstack, cards.tcpBytes());
				
				int multiple = 1;
				// this line is the start of a new round
				while (index != lastplay) {
					byte topcard = cards.topcard();
					
					SClient c = ingame.get(index);
					broadcast(pc_playerturn, c.idbytes);
					Deck playable = c.currentCards.getPlayable(topcard);
					updateCards(ingame, cards);
					if (!c.canPlay(topcard, multiple)) {
						broadcast(pc_playerpass, c.idbytes);
					} else {
						try {
							if (c.currentCards.ncards <= 0) {
								order.add(c);
								ingame.remove(c);
								continue;
							}
							if (cards.ncards == NULL) {
								multiple = c.getMultiple();
								if (multiple < 1) multiple = 1;
								if (multiple > 3) multiple = 1;
							}
							byte cardindex = c.getCardindex(topcard, multiple, cards.ncards == 0);
							if (cardindex == card_pass) {
								broadcast(pc_playerpass, c.idbytes);
							} else {
								byte card = playable.cards[cardindex % playable.ncards];
								int numRemoved = 0;
								
								int k = 0;
								while (numRemoved < multiple) {
									numRemoved += c.currentCards.remove(card + k++);
								}
								
								if (c.currentCards.ncards <= 0) {
									order.add(c);
									ingame.remove(c);
									continue;
								}
								
								if (card / 4 == 0) { // for bombs
									cards = new Deck(new byte[0]);
									broadcast(pc_bomb, c.idbytes);
									index -= 1;
									lastplay = -1;
								} else { // for actually playing another card
									lastplay = index;
									if (multiple >= 2) {
										if (multiple == 2) {
											if (card / 4 == cards.topcard() / 4) {
												completion(c);
											}
										}
										
									} else {
										if (card / 4 == cards.topcard() / 4) {
											index += 1;
											broadcast(pc_skip, c.idbytes);
										}
									}
									for (int i = 0; i < multiple; i++) {
										cards.add(card);
									}
									
								}
							}
							
							updateCards(ingame, cards);
							index = (index + 1) % ingame.size();
						} catch (InterruptedException e) {
							completionClient.currentCards = Deck.complete(cards, completionClient.currentCards); // this only occurs if the completion() method determines it
							cards = new Deck(new byte[0]);
							lastplay = -1;
							index = ingame.indexOf(completionClient);
							updateCards(ingame, cards);
						}
					} // placing a card
					
					if (c.scumOut(cards.ncards == 0)) {
						broadcast(pc_scumout, c.idbytes);
						ingame.remove(c);
						scumout.add(c);
					} // checking if they scum out
				} // current pile
			} // game loop while
			order.addAll(scumout);
			for (int i = 0; i < order.size(); i++) {
				broadcast(pc_playerpos, Packet.longToBytes(i), Packet.longToBytes(order.size()), order.get(i).idbytes);
			}
			
			
		} catch (Exception e) {
			broadcast(pc_message, ("[Server] Sorry, there has been a server-side error. ").getBytes());
			window.chat.println(e.getMessage());
			e.printStackTrace();
		} // main round try clause
	}
	
	public void completion(SClient client) {
		if (Deck.canComplete(cards, client.currentCards)) {
			completionClient = client;
			client.currentCards = Deck.complete(cards, client.currentCards);
			interrupt();
			broadcast(pc_completion, client.idbytes);
		} else {
			client.outpacks.send(pc_message, "You can't complete that.".getBytes());
		}
	}
	
	public void autoCompletion(SClient client, byte card) {
		if (client.currentCards.canAuto(card)) {
			client.currentCards = client.currentCards.auto(card);
			interrupt();
			broadcast(pc_auto, Packet.longToBytes(client.id));
		}
	}
	/**
	 * Tells clients their current cards, their playable cards, and the current pile
	 * @param clients
	 * @param pile
	 */
	public void updateCards(CopyOnWriteArrayList<SClient> clients, Deck pile) {
		for (SClient s : clients) {
			s.outpacks.send(pc_cardstack, pile.tcpBytes());
			s.outpacks.send(pc_yourcards, s.currentCards.tcpBytes());
			s.outpacks.send(pc_playable, s.currentCards.getPlayable(pile.topcard()).tcpBytes());
		}
	}
	
	public void broadcast(byte messageType, byte[]... content) {
		broadcast(messageType, clients, content);
	}
	
	public void broadcast(byte messageType, CopyOnWriteArrayList<SClient> clients, byte[]... content) {
		//window.chat.println("Broadcast " + messageType + "_" + DebugUtil.bstr(Packet.ccat(content)));
		if (messageType == pc_message) {
			//window.chat.println(new String(Packet.ccat(content)));
		}
		for (SClient to : clients) {
			to.outpacks.send(messageType, content);
		}
	}
	
	
	
	public static void main(String[] args) throws FileNotFoundException {
		
		//*
		Server server = new Server(5555);
		server.startServer();
		System.setErr(new PrintStream(new FileOutputStream("server.log")));
		//*/
		
		/*
		Deck d = new Deck(new byte[] {4});
		Deck d2 = new Deck(new byte[] {5,6,7});
		System.out.println(Deck.complete(d, d2));
		//*/
	}
	public static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}
}

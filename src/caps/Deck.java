package caps;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A wrapper class for a deck of cards.
 * Includes functions for sending over the tcp connection.
 * Has functions to convert card id to string.
 * @author legoc
 *
 */

public class Deck {
	public byte[] cards;
	public int ncards = 52;
	public static final String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
	public static final char[] numbers = {'2','3','4','5','6','7','8','9','0','J','Q','K','A'};
	// suit order: hearts, diamonds, clubs, spades.
	public Deck() { // creates new "Deck" with 52 cards
		cards = new byte[52];
		for (byte i = 0; i < 52; i++) {
			cards[i] = i;
		}
	}
	
	public Deck(byte[] cards) {
		this.cards = cards;
		ncards = cards.length;
	}
	
	public String toString() {
		String str = "Deck of cards: ";
		
		for (int i = 0; i < cards.length; i++) {
			str += cardToString(cards[i]);
			if (i < cards.length - 1) {
				str += ", ";
			}
		}
		
		return str;
	}
	
	public void shuffle() {
		shuffleArray(cards);
	}
	
	public byte[] tcpBytes() {
		byte[] content = new byte[ncards + 1];
		content[0] = (byte)ncards;
		for (int i = 0; i < ncards; i++) {
			content[i + 1] = cards[i];
		}
		return content;
	}
	
	public static Deck parseTCP(byte[] tcp) {
		byte[] cards = new byte[tcp[0]];
		for (int i = 0; i < tcp[0]; i++) {
			cards[i] = tcp[i + 1];
		}
		return new Deck(cards);
	}
	
	public static Deck shuffled() {
		Deck d = new Deck();
		d.shuffle();
		return d;
	}
	
	public static Deck unshuffled() {
		return new Deck();
	}
	
	public Deck[] deal(int people) {
		Deck[] d = new Deck[people];
		for (int i = 0; i < people; i++) {
			d[i] = new Deck();
			d[i].ncards = 0;
		}
		for (int i = 0; i < ncards; i++) {
			Deck c = d[i % people];
			c.cards[c.ncards++] = cards[i];
		}
		return d;
	}
	public Deck[] dealEvenly(int people) {
		Deck[] d = new Deck[people];
		for (int i = 0; i < people; i++) {
			d[i] = new Deck();
			d[i].ncards = 0;
		}
		for (int i = 0; i < (ncards/people * people); i++) {
			Deck c = d[i % people];
			c.cards[c.ncards++] = cards[i];
		}
		return d;
	}
	public static String cardToString(byte card) {
		try {
			int suit = card % 4;
			int number = card / 4;
			
			return numbers[number] + " of " + suits[suit];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "Empty";
		}
		
	}
	
	public Deck getPlayable(int card) {
		Deck d = new Deck(new byte[ncards]);
		d.ncards = 0;
		for (int i = 0; i < ncards; i++) {
			if (cards[i] / 4 == 0 || cards[i] / 4 >= card / 4) {
				d.cards[d.ncards] = cards[i];
				d.ncards += 1;
			}
		}
		return d;
	}
	/**
	 * Removes all instances of a certain card in the deck, and readjusts the deck accordingly. If there are no instances, no changes are made.
	 * @param card
	 */
	public int remove(int card) {
		int count = 0;
		for (int i = 0; i < ncards; i++) {
			if (cards[i] == card) {
				count++;
				byte[] total = new byte[ncards - 1];
				System.arraycopy(cards, 0, total, 0, i);
				System.arraycopy(cards, i + 1, total, i, ncards - i - 1);
				this.cards = total;
				this.ncards -= 1;
			}
		}
		return count;
	}
	
	public void add(int card) {
		byte[] total = new byte[ncards + 1];
		for (int i = 0; i < ncards; i++) {
			total[i] = this.cards[i];
		}
		total[ncards] = (byte)card;
		this.cards = total;
		this.ncards += 1;
	}
	
	/**
	 * 
	 * @return the last card in this deck. If this deck has no cards, return -1;
	 */
	public byte topcard() {
		if (this.ncards > 0) {
			return this.cards[this.ncards - 1];
		} else {
			return -1;
		}
	}
	
	// Implementing Fisher–Yates shuffle
	public static void shuffleArray(byte[] ar) {
		// If running on Java 6 or older, use `new Random()` on RHS here
		Random rnd = ThreadLocalRandom.current();
		for (int i = ar.length - 1; i > 0; i--)
		{
			int index = rnd.nextInt(i + 1);
			// Simple swap
			byte a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}
	
	@Override
	public Deck clone() {
		Deck d = new Deck();
		byte[] tmp = new byte[this.ncards];
		d.ncards = this.ncards;
		for (int i = 0; i < Math.min(d.ncards, this.cards.length); i++) {
			tmp[i] = this.cards[i];
		}
		d.cards = tmp;
		return d;
	}
	
	public String toNumberedString() {
		String str = new String();
		for (int i = 0; i < Math.min(ncards, cards.length); i++) {
			str += "[" + (i) + "] " + cardToString(cards[i]) + "\n";
		}
		return str;
	}
	
	public Deck sorted() {
		Deck tmp = this.clone();
		Arrays.sort(tmp.cards);
		return tmp;
	}
	
	public static boolean canComplete(Deck top, Deck completeWith) {
		if (completeWith.ncards == 0) return false;
		if (top.ncards == 0) return false;
		if (completeWith.ncards + top.ncards < 4) return false;
		int card = top.topcard();
		int ncards = 0; // number of cards at top of deck
		int ncardscm = 0; // number of cards in completewith deck
		
		for (int i = top.ncards - 1; i >= 0 && top.cards[i] / 4 == card / 4; i--) {
			ncards += 1;
		}
		for (int c : completeWith.cards) {
			if (c / 4 == card / 4) {
				ncardscm += 1;
			}
		}
		
		return ncards + ncardscm == 4;
	}
	
	public boolean canAuto(byte card) {
		int ncards = 0;
		for (byte c : cards) {
			if (c / 4 == card / 4) {
				ncards += 1;
			}
		}
		return ncards >= 4;
	}
	
	public Deck auto(byte card) {
		Deck tmp = clone();
		for (int i = card * 13; i < card * 13 + 4; i++) {
			tmp.remove(i);
		}
		return tmp;
	}
	
	/**
	 * Assuming that they can be completed.
	 * @param top the deck to complete
	 * @param completeWith the deck to use to complete
	 * @return the remaining deck after completion
	 */
	
	public static Deck complete(Deck top, Deck completeWith) {
		if (Deck.canComplete(top, completeWith)) {
			Deck tmp = completeWith.clone();
			int card = top.cards[top.ncards - 1] / 4;
			for (int i = 0; i < 4; i++) {
				tmp.remove(4 * card + i);
			}
			return tmp;
		} else {
			return completeWith;
		}
	}
}

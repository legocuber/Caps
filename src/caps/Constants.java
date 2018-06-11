package caps;

import java.awt.Font;

public class Constants {
	/**
	 * Format: {keepalive:}
	 */
	public static final byte pc_keepalive = 0x00;
	
	/**
	 * Format: {datarequest:datapiece}
	 */
	public static final byte pc_datarequest = 0x01;
	
	/**
	 * Format: {timewarning:deadline}
	 */
	public static final byte pc_timewarning = 0x02;
	
	/**
	 * Format: {message:userid-x8;content}
	 */
	public static final byte pc_message = 0x03;
	
	/**
	 * Format: {yourid:userid-x8}
	 */
	public static final byte pc_yourid = 0x04;
	
	/**
	 * Format: {cardstack:deck}
	 */
	public static final byte pc_cardstack = 0x05;
	
	/**
	 * Format: {cardtoplay:card-x8}
	 */
	public static final byte pc_cardtoplay = 0x06;
	
	/**
	 * Format: {playerdc:userid-x8}
	 */
	public static final byte pc_playerdc = 0x07;
	
	/**
	 * Format: {playerinfo:userid-x8;username}
	 */
	public static final byte pc_playerinfo = 0x08;
	
	/**
	 * Format: {gamestatus:status}
	 */
	public static final byte pc_gamestatus = 0x09;
		public static final byte pc_gameend = 0x00;
		public static final byte pc_gamestart = 0x01;
	
	/**
	 * The receiver's current card hand.
	 * Format: {yourcards:deck}
	 */
	public static final byte pc_yourcards = 0x0A;
	
	/**
	 * Packet saying the player chooses to pass.
	 * Format: {pass:}
	 */
	public static final byte pc_pass = 0x0B;
	
	/**
	 * Card ID: Pass
	 */
	public static final byte card_pass = -2;
	
	/**
	 * Same as `mypos`
	 * Format: {mypos:place-x8,nplayers-x8,playerid-x8}
	 */
	public static final byte pc_playerpos = 0x0D;
	
	/**
	 * Tells players that someone was skipped.
	 * Format: {playerskip:playerid-x8}
	 */
	public static final byte pc_playerskip = 0x0E;
	
	/**
	 * Tells players whose turn it is.
	 * Format: {playerturn:playerid-x8}
	 */
	public static final byte pc_playerturn = 0x0F;
	
	/**
	 * Tells players that somebody passed.
	 * Format: {playerpass:playerid-x8}
	 */
	public static final byte pc_playerpass = 0x10;
	
	/**
	 * Tells server that they can complete the set of four
	 * Format to server: {completion:}
	 * Format from server: {completion:playerid-x8}
	 */
	public static final byte pc_completion = 0x11;
	
	/**
	 * Tells players that a person has scummed out.
	 * Format: {scumout:player}
	 */
	public static final byte pc_scumout = 0x12;
	
	/**
	 * Tells a player which cards they can play.
	 * Format: {playable:deck}
	 */
	public static final byte pc_playable = 0x13;
	
	/**
	 * Tells players that a bomb has been played.
	 * Format: {bomb:playerid-x8}
	 */
	public static final byte pc_bomb = 0x14;
	
	/**
	 * Tells players that an autocompletion has occurred.
	 * Format: {auto:playerid-x8}
	 */
	public static final byte pc_auto = 0x15;
	
	/**
	 * Tells players that someone was skipped
	 * Format: {skip:skipper}
	 */
	public static final byte pc_skip = 0x16;
	
	/** Tells server how many cards to play
	 * Format: {multiple:count-x8}
	 */
	public static final byte pc_multiple = 0x17;
		/*
		 * Data Payload Formats
		 * 
		 * Name: Bytestring
		 * ID: 8-Bytes Long
		 * 
		 * PlayerID: 8-Bytes Long
		 * PlayerName: (PlayerID, Bytestring)
		 * 
		 * TurnWarning: 8-Bytes Long, Time in millis of autoplay.
		 * AutoPlay: Tells you that the server played for you.
		 * 
		 * 
		 */
	public static final Font consolas = new Font("Consolas", Font.PLAIN, 12);
}

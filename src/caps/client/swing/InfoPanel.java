package caps.client.swing;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import caps.Constants;
import caps.Deck;
import caps.client.CClient;

public class InfoPanel extends JPanel {
	public CClient parent = null;
	public JTextArea stack = new JTextArea(5, 20);
	public JTextArea mycards = new JTextArea(5, 20);
	public JTextArea playable = new JTextArea(4, 20);
	public JScrollPane scrollStack;
	public JScrollPane scrollMyCards;
	public JScrollPane scrollPlayable;
	public InfoPanel(CClient c) {
		super();
		this.setLayout(new GridLayout(0, 1));
		this.setBorder(new TitledBorder(new EtchedBorder(), "Cards"));
		parent = c;
		
		stack.setBorder(new TitledBorder(new EtchedBorder(), "Card pile"));
		mycards.setBorder(new TitledBorder(new EtchedBorder(), "My cards"));
		playable.setBorder(new TitledBorder(new EtchedBorder(), "Playable cards"));
		
		stack.setFont(Constants.consolas);
		mycards.setFont(Constants.consolas);
		playable.setFont(Constants.consolas);
		
		
		scrollStack = new JScrollPane(stack);
		scrollStack.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollMyCards = new JScrollPane(mycards);
		scrollMyCards.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPlayable = new JScrollPane(playable);
		scrollPlayable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		this.add(scrollStack);
		this.add(scrollMyCards);
		this.add(scrollPlayable);
		
		stack.setEditable(false);
		mycards.setEditable(false);
	}
	public void setCards(JTextArea area, Deck d) {
		area.setEditable(true);
		area.setText(d.toNumberedString());
		area.setEditable(false);
	}
	
}
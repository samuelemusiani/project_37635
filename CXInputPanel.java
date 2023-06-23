/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx;

//import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
//import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;



import connectx.CXGame.CXPlayerType;

public class CXInputPanel extends JPanel implements ActionListener, Serializable {

	CXBoardPanel bPanel;
	JLabel player1;
	JLabel player2;
	JLabel score;
	JLabel turn;
	JLabel PL1;
	JLabel PL2;
	JLabel labelSP1;
	JLabel labelSP2;
	JLabel SP1;
	JLabel SP2;

	// Stroke defaultStroke;
	// protected static int SYMBOL_STROKE_WIDTH = 10;

	private static final long serialVersionUID = 1L;
	private int data;

	public CXInputPanel(Border bord, CXBoardPanel p) {

		bPanel = p;
		setBorder(bord);
		setLayout(new GridLayout(getComponentCount(), 1, 10, 10));
		// setBackground(new Color(255, 255, 255));
		setBackground(Color.BLUE);
		setPreferredSize(new Dimension(120, getHeight()));
		// setMaximumSize(getPreferredSize());

		JButton ex = new JButton("EXIT");
		ex.addActionListener(this);

		JButton reset = new JButton("RESET");
		reset.addActionListener(this);

		add(ex);
		add(reset);

		turn = new JLabel();
		turn = new JLabel("TURN", SwingConstants.CENTER);
		turn.setForeground(Color.WHITE);
		turn.setFont(new Font("Arial", Font.BOLD, 20));
		add(turn);

		player1 = new JLabel("Player 1", SwingConstants.CENTER);
		player1.setFont(new Font("Arial", Font.BOLD, 18));
		player1.setForeground(Color.WHITE);
		add(player1);

		player2 = new JLabel("Player 2", SwingConstants.CENTER);
		player2.setFont(new Font("Arial", Font.BOLD, 18));
		player2.setForeground(Color.WHITE);
		add(player2);

		score = new JLabel("Score", SwingConstants.CENTER);
		score.setFont(new Font("Arial", Font.BOLD, 20));
		score.setForeground(Color.WHITE);
		add(score);

		PL1 = new JLabel("", SwingConstants.CENTER);
		PL1.setFont(new Font("Serif", Font.CENTER_BASELINE, 14));
		add(PL1);

		PL2 = new JLabel("", SwingConstants.CENTER);
		PL2.setFont(new Font("Serif", Font.CENTER_BASELINE, 14));
		add(PL2);

		SP1 = new JLabel("", SwingConstants.CENTER);
		SP1.setFont(new Font("Serif", Font.CENTER_BASELINE, 18));
		add(SP1);

		labelSP1 = new JLabel("", SwingConstants.CENTER);
		labelSP1.setFont(new Font("Serif", Font.CENTER_BASELINE, 14));
		add(labelSP1);

		SP2 = new JLabel("", SwingConstants.CENTER);
		SP2.setFont(new Font("Serif", Font.CENTER_BASELINE, 18));
		add(SP2);

		labelSP2 = new JLabel("", SwingConstants.CENTER);
		labelSP2.setFont(new Font("Serif", Font.CENTER_BASELINE, 14));
		add(labelSP2);

		// JButton clear = new JButton("Clear");
		// clear.addActionListener(this);
		// add(clear);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		// defaultStroke = g2.getStroke();
		if (bPanel.board.currentPlayer == CXBoardPanel.PLAYER1)
			g2.setPaint(new GradientPaint(75, 75, new Color(255, 255, 0), 100, 100, new Color(200, 200, 0), true));
		else
			g2.setPaint(new GradientPaint(75, 75, new Color(220, 0, 0), 100, 100, new Color(150, 0, 0), true));

		String P1 = bPanel.Player[0] == CXPlayerType.HUMAN ? "Human" : bPanel.ComPlayer[0].playerName();
		String P2 = bPanel.Player[1] == CXPlayerType.HUMAN ? "Human" : bPanel.ComPlayer[1].playerName();

		if (bPanel.NUMBER_OF_ROWS < getComponentCount()) {
			player1.setBounds(turn.getHorizontalTextPosition(), turn.getY() + turn.getHeight() + 60, 100, 15);
			player2.setBounds(player1.getHorizontalTextPosition(), player1.getY() + player1.getHeight() + 40, 100, 15);
			score.setBounds(player2.getHorizontalTextPosition(), player2.getY() + player2.getHeight() + 40, 100, 15);

			PL1.setText(P1);
			PL1.setBounds(player1.getHorizontalTextPosition(), player1.getY() + (player1.getHeight() + 15), 100, 15);

			// PL2.setPreferredSize(new Dimension(80,getHeight()));
			PL2.setText(P2);
			PL2.setBounds(player2.getHorizontalTextPosition(), player2.getY() + (player2.getHeight() + 15), 100, 15);

			labelSP1.setText(P1);
			labelSP1.setBounds(score.getHorizontalTextPosition(), score.getY() + (score.getHeight() + 15), 100, 15);
			SP1.setText("" + bPanel.ScorePlayer[0]);
			SP1.setBounds(score.getHorizontalTextPosition(), labelSP1.getY() + (labelSP1.getHeight() + 10), 100, 15);

			labelSP2.setText(P2);
			labelSP2.setBounds(SP1.getHorizontalTextPosition(), SP1.getY() + (SP1.getHeight() + 20), 100, 15);
			SP2.setText("" + bPanel.ScorePlayer[1]);
			SP2.setBounds(SP1.getHorizontalTextPosition(), labelSP2.getY() + (labelSP2.getHeight() + 10), 100, 15);

			if (bPanel.NUMBER_OF_ROWS <= 4) {

				g2.fillOval(getWidth() / 4, turn.getY() + turn.getHeight(), 60, 60);
			} else {
				g2.fillOval(getWidth() / 4, turn.getY() + turn.getHeight() - bPanel.NUMBER_OF_ROWS, 60, 60);
			}

		} else {
			PL1.setText(P1);
			PL1.setBounds(player2.getHorizontalTextPosition(), player1.getY() + (player1.getHeight() - 15), 100, 15);

			// PL2.setPreferredSize(new Dimension(80,getHeight()));
			PL2.setText(P2);
			PL2.setBounds(player2.getHorizontalTextPosition(), player2.getY() + (player2.getHeight() - 15), 100, 15);

			labelSP1.setText(P1);
			labelSP1.setBounds(score.getHorizontalTextPosition(), score.getY() + (score.getHeight() - 20), 100, 15);
			SP1.setText("" + bPanel.ScorePlayer[0]);
			SP1.setBounds(score.getHorizontalTextPosition(), labelSP1.getY() + (labelSP1.getHeight() + 10), 100, 15);

			labelSP2.setText(P2);
			labelSP2.setBounds(SP1.getHorizontalTextPosition(), SP1.getY() + (SP1.getHeight() + 20), 100, 15);
			SP2.setText("" + bPanel.ScorePlayer[1]);
			SP2.setBounds(SP1.getHorizontalTextPosition(), labelSP2.getY() + (labelSP2.getHeight() + 10), 100, 15);

			if (bPanel.NUMBER_OF_ROWS == getComponentCount()) {

				g2.fillOval(getWidth() / 4, turn.getY() + turn.getHeight(), 60, 60);
			} else {
				g2.fillOval(getWidth() / 4, turn.getY() + turn.getHeight() - bPanel.NUMBER_OF_ROWS, 60, 60);
			}

		}

		repaint();

		/*
		 * g2.setColor(Color.BLUE);
		 * 
		 * g2.setStroke(new BasicStroke(SYMBOL_STROKE_WIDTH, BasicStroke.CAP_ROUND,
		 * BasicStroke.JOIN_ROUND)); g2.drawOval(getWidth() / 5, ((getHeight() /
		 * (bPanel.NUMBER_OF_ROWS + 1)) * (getComponentCount())), 62, 62);
		 * g2.setStroke(defaultStroke);
		 */
	}

	public void actionPerformed(ActionEvent ae) {
		String buttonStr = ((JButton) (ae.getSource())).getText();
		if (buttonStr.equals("EXIT"))
			System.exit(0);
		if (buttonStr.equals("RESET")) {

			CXGame.game.initGame();
			bPanel.gameState = CXGameState.OPEN;
			bPanel.initScore();
			bPanel.initiateAnimationState();
		}
		/*
		 * if (buttonStr.equals("clear")) { bPanel.ScorePlayer[0]=0;
		 * bPanel.ScorePlayer[1]=0; }
		 */
	}

	public void SerializeMe(int data) {
		this.data = data;
	}

	public int getData() {
		return data;
	}
	
}

 

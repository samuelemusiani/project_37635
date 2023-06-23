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

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import connectx.CXGame.CXGameType;
import connectx.CXGame.CXPlayerType;

/**
 * Inner class for custom graphics drawing.
 */
public class CXBoardPanel extends JPanel implements MouseListener, MouseMotionListener, Serializable {
	protected final int NUMBER_OF_ROWS;
	protected final int NUMBER_OF_COLS;

	protected final int WIN_ANIMATION_FRAMES;

	private static final int TIMEOUT = 10;

	public static final int PLAYER1 = 0; // PLAYER1
	public static final int PLAYER2 = 1; // PLAYER2

	/** Scoring system */
	private static int WINSCORE  = 3;
	private static int DRAWSCORE = 1;

	protected int[] ScorePlayer = new int[2];

	public int Board_Top_Border; // BOARD_TOP_BORDER

	protected CXPlayerType[] Player;
	protected CXPlayer[] ComPlayer;

	protected CXGameType gameType;
	// Stroke defaultStroke;
	CXGameState gameState;
	CXBoard board;
	protected JLabel statusBar;

	/* for drawing the grid */
	int cellGap;
	int boardWidth;
	int boardHeight;
	int extraBorder;

	/* for animating coin drop */
	int textAnimationInt; // used to control movement of text and text box across screen (want to stop in
							// middle)
	boolean animatingCoinDrop;
	int animationColumn;
	int animationRow;
	int animationFrame;
	int animationStartPosition;
	double animationFrameAcceleration;

	/*
	 * control mouse pointer which is the ball about to be dropped - pointer
	 * position is limited.
	 */
	boolean drawPointerOnMouse = false; // when mouse is in critical zone a piece is shown around it.
	int mousePointerHorizontalPosition = 0;
	int mousePointerVerticalPosition = 0; // this and above are all set on mouseMove

	/* are used to monitor the game situation and animate accordingly */
	int winAnimationFrame;
	int ovalWidth;
	int ovalWidthModifier = 1;
	boolean fullColum = false;
	boolean winFirstDetection = true;

	Image dbImage; // for double buffering

	/* gradient used for drawing pointers. */
	GradientPaint gpYellow; // for the yellow player's moves
	GradientPaint gpRed; // for the red player's moves

	/* seriaisable */
	private static final long serialVersionUID = 1L;

	public CXBoardPanel(CXBoard board, CXGameType type, Border bord, int cell_size, CXPlayer[] ComPlayer,
			CXPlayerType[] Player, JLabel statusBar) {

		gameState = board.gameState;
		gameType = type;

		this.board = board;
		this.ComPlayer = ComPlayer;
		this.Player = Player;
		this.statusBar = statusBar;
		NUMBER_OF_ROWS = this.board.M;
		NUMBER_OF_COLS = this.board.N;
		cellGap = cell_size;
		WIN_ANIMATION_FRAMES = cellGap * 4;
		setLayout(new GridLayout(1, 1, 5, 5)); // a single cell
		// setBorder(bord);
		// setBackground(Color.RED);
		addMouseListener(this);
		addMouseMotionListener(this);
		initScore();
		initiateAnimationState(); // set the initial parameters for the coin animation

	}

	public void initScore() {
		if (ScorePlayer == null) {
			ScorePlayer[0] = board.currentPlayer();
			ScorePlayer[1] = board.currentPlayer() % 1;
		} else {
			int tmp = ScorePlayer[0];
			ScorePlayer[0] = ScorePlayer[1];
			ScorePlayer[1] = tmp;
		}
		winFirstDetection = true;
	}

	public void initiateAnimationState() {
		ovalWidth = cellGap - 18;
		setAnimationState(false, 0, 0, 0);
		winAnimationFrame = 0;
	}

	public void setAnimationState(boolean b, int col, int rowBegin, int a) {
		/* variables are reset every time a coin is dropped */
		animationFrame = 0;
		animationFrameAcceleration = 1;
		animationColumn = col;
		animatingCoinDrop = b;
		animationStartPosition = rowBegin; // this is the position of the mouse pointer - drops from the pointer
		animationRow = a; // last move in Game is the one we are animating.
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		effectDoubleBuffering(g2d);

		// Print status-bar message
		switch (board.gameState()) {
		case OPEN:
			statusBar.setForeground(Color.BLACK);
			String colorPlayer = board.currentPlayer() == 0 ? "Yellow" : "Red";
			String msg = Player[board.currentPlayer()] == CXPlayerType.COMPUTER ? "Click to run"
					: "Click on white bar to select column";
			String name = Player[board.currentPlayer()] == CXPlayerType.COMPUTER
					? ComPlayer[board.currentPlayer()].playerName()
					: "Human";
			statusBar.setText(colorPlayer + "'s Turn (" + name + ") - " + msg);
			break;
		case DRAW:
			statusBar.setForeground(Color.RED);
			statusBar.setText("Draw! Click reset to play again.");
			break;
		case WINP1:
			String name1 = Player[0] == CXPlayerType.COMPUTER ? ComPlayer[0].playerName() : "Human";
			statusBar.setForeground(Color.RED);
			statusBar.setText("Yellow (" + name1 + ") Won! Click reset to play again.");
			break;
		case WINP2:
			String name2 = Player[1] == CXPlayerType.COMPUTER ? ComPlayer[1].playerName() : "Human";
			statusBar.setForeground(Color.RED);
			statusBar.setText("Red (" + name2 + ") Won! Click reset to play again.");
			break;
		}
		repaint();
	}

	public void effectDoubleBuffering(Graphics2D g2) {
		if (dbImage == null) {
			dbImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		}

		Graphics2D offG = (Graphics2D) (dbImage.getGraphics()); // clear the screen

		Board_Top_Border = (getHeight() - CXGame.EXTRA_ORIZONTAL_BORDER);
		boardWidth = getWidth();
		boardHeight = getHeight();
		extraBorder = boardHeight - Board_Top_Border - (cellGap * NUMBER_OF_ROWS);

		makeNextFrame(offG); // Put the dbImage image on the screen.

		g2.drawImage(dbImage, 0, 0, null);
	}

	public void makeNextFrame(Graphics2D g2) {

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// defaultStroke = g2.getStroke();
		g2.setPaint(Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());

		drawGameBoard(g2);
	}

	/* draw grid board */
	private void drawShapeCells(Graphics2D g2) {
		for (int i = 0; i < NUMBER_OF_ROWS; i++) {
			for (int j = 0; j < NUMBER_OF_COLS; j++) {
				Area af2 = new Area(cell(j * cellGap, i * cellGap + Board_Top_Border + extraBorder, cellGap));
				af2.subtract(new Area(new Ellipse2D.Float(j * cellGap + 5,
						i * cellGap + Board_Top_Border + extraBorder + 5, cellGap - 10, cellGap - 10)));
				g2.setColor(Color.BLUE.darker().darker());
				g2.fill(af2);
			}
		}
	}

	/* draw grid board, draw coins(marked cells) and animate win */
	private void drawGameBoard(Graphics2D g2) {

		if (animatingCoinDrop)
			animateCoinDrop(g2);
		if (drawPointerOnMouse)
			drawMousePointer(g2);

		drawShapeCells(g2);

		CXCell[] list = board.getMarkedCells();

		drawCounters(g2, list);

		if ((gameState == CXGameState.WINP1 || gameState == CXGameState.WINP2) && (!animatingCoinDrop)) {
			CXCell m = board.getLastMove();
			animateWin(g2, winCells(m.i, m.j));

			if (winFirstDetection) {
				winFirstDetection = false;

				switch (gameState) {
					case WINP1:
						ScorePlayer[0] += WINSCORE;
						break;
					case WINP2:
						ScorePlayer[1] += WINSCORE;
						break;
					default:
						break;
				}
			}
		} else if (gameState == CXGameState.DRAW && (!animatingCoinDrop)) {
			animateGameOverText(g2, textAnimationInt);
			if (winFirstDetection) {
				winFirstDetection = false;
				ScorePlayer[0] += DRAWSCORE;
				ScorePlayer[1] += DRAWSCORE;
			}

			if (textAnimationInt < (boardWidth / 2) - 100)
				textAnimationInt = textAnimationInt + NUMBER_OF_COLS;
		}
	}

	private void animateWin(Graphics2D g2, LinkedList<CXCell> moves) {

		// remove the winning counters from the board (so they don't get
		// drawn there as well as here)
		Iterator<CXCell> iteratore = moves.iterator();
		while (iteratore.hasNext()) {

			CXCell m = iteratore.next();

			int x = m.j * cellGap + 5;
			int y = m.i * cellGap + (Board_Top_Border + extraBorder + 5);

			g2.setPaint(Color.WHITE);
			g2.create().fillOval(x, y, cellGap - 10, cellGap - 10);
		}

		if (board.currentPlayer == PLAYER1)
			g2.setPaint(getRedGradientPaint());
		else
			g2.setPaint(getYellowGradientPaint());

		/*
		 * winning pieces to spin - code below sorts this: ovalWidthModifier begins at 1
		 * but ovalWidth begins at cellGap, so first if is bypassed and
		 * ovalWidthModifier is set to 0 ovalwidth increases until oval is full sized
		 * cellGap.
		 */

		if ((ovalWidth == cellGap) && (winAnimationFrame < WIN_ANIMATION_FRAMES))
			ovalWidthModifier = 1;

		// when ovalwidth hits cellGap the width of the oval will decrease until
		// it again hits cellGap.
		if (ovalWidth == 0)
			ovalWidthModifier = 0;

		if (ovalWidthModifier == 1)
			ovalWidth = winAnimationFrame % cellGap;

		if (ovalWidthModifier == 0)
			ovalWidth = cellGap - winAnimationFrame % cellGap;

		if (ovalWidth == cellGap)
			ovalWidth = 0;
		else if (ovalWidth == 0)
			ovalWidth = cellGap;

		for (CXCell c : moves) 
			g2.fillOval(cellGap * c.j + 5 + (cellGap / 2 - ovalWidth / 2), Board_Top_Border + extraBorder + cellGap * c.i + 5, ovalWidth - 10, cellGap - 10);

		// when game is won a token is added to winning pieces
		if (winAnimationFrame >= WIN_ANIMATION_FRAMES) {
			for (CXCell c : moves) {
				if (board.currentPlayer == PLAYER1)
					g2.setPaint(getRedGradientPaint());
				else
					g2.setPaint(getYellowGradientPaint());

				g2.fillOval(cellGap * c.j + 5, Board_Top_Border + extraBorder + cellGap * c.i + 5, cellGap - 10, cellGap - 10);

				if (cellGap == 60) {
					g2.setPaint(Color.GREEN.darker());
					g2.drawString("Win!", cellGap * c.j + (cellGap / 5) + 5, Board_Top_Border + extraBorder + cellGap * c.i + (cellGap / 2) + 5);
				} else if (cellGap == 45) {
					g2.setPaint(Color.GREEN.darker());
					g2.drawString("Win!", cellGap * c.j + (cellGap / 9) + 5, Board_Top_Border + extraBorder + cellGap * c.i + (cellGap / 2) + 5);
				} else {
					g2.setPaint(Color.GREEN.darker());
					g2.drawString("Win!", cellGap * c.j + (cellGap / 3) + 5, Board_Top_Border + extraBorder + cellGap * c.i + (cellGap / 2) + 5);
				}
			}
		}

		/*
		 * solution to the issue of flicker in the ovals - modular div letting me down
		 * on boundaries. Below ensures that there is a smooth transition between
		 * boundaries.
		 */
		if (ovalWidth == 0)
			ovalWidth = cellGap;
		else if ((ovalWidth == cellGap) && (winAnimationFrame < WIN_ANIMATION_FRAMES))
			ovalWidth = 0;

		animateGameOverText(g2, textAnimationInt);

		if (winAnimationFrame < WIN_ANIMATION_FRAMES)
			winAnimationFrame = winAnimationFrame + NUMBER_OF_ROWS;
		if (textAnimationInt < (boardWidth / 2) - 100)
			textAnimationInt = textAnimationInt + NUMBER_OF_COLS;
	}

	/* Game Over copy below moves from right to left until it is centralized */
	private void animateGameOverText(Graphics2D g2, int textAnimationInt) {
		int len;
		g2.setPaint(Color.DARK_GRAY);
		g2.fillRect(0 + textAnimationInt, ((boardHeight / 2)), 200, 100);
		g2.setPaint(Color.white);
		g2.setFont(new Font("Arial", Font.BOLD, 28));
		g2.drawString("Game over", 30 + textAnimationInt, (boardHeight / 2) + 30);
		if (gameState == CXGameState.WINP1)
			g2.setPaint(getYellowGradientPaint());
		else
			g2.setPaint(getRedGradientPaint());

		g2.setFont(new Font("Arial", Font.BOLD, 22));
		len = getMoveResultText(gameState).length();
		g2.drawString(getMoveResultText(gameState), (200 / len) + textAnimationInt, (boardHeight / 2) + 80);
	}

	public String getMoveResultText(CXGameState moveResult) {
		switch (moveResult) {
		case WINP1:
			return "Yellow player wins";
		case WINP2:
			return "Red player wins";
		case DRAW:
			return "DRAW!";
		case OPEN:
			return "Error";
		}
		return "Error";
	}

	/*
	 * Check winning state from cell i, j, return win list moves
	 */
	private LinkedList<CXCell> winCells(int i, int j) {
		CXCellState s = board.cellState(i, j);
		LinkedList<CXCell> moves = new LinkedList<CXCell>();

		int n;

		// Horizontal check
		n = 1;
		for (int h = 1; j - h >= 0 && board.cellState(i, j - h) == s; h++) {
			n++;
			moves.add(new CXCell(i, j - h, s));
		} // backward check
		for (int h = 1; j + h < board.N && board.cellState(i, j + h) == s; h++) {
			n++;
			moves.add(new CXCell(i, j + h, s));
		} // forward check
		if (n >= board.X) {
			moves.add(new CXCell(i, j, s));
			return moves;
		}

		// Vertical check
		n = 1;
		moves.clear();
		for (int h = 1; i + h < board.M  && board.cellState(i + h, j) == s; h++) {
			n++;
			moves.add(new CXCell(i + h, j, s));
		} // forward check
		if (n >= board.X) {
			moves.add(new CXCell(i, j, s));
			return moves;
		}

		// Diagonal check
		n = 1;
		moves.clear();
		for (int h = 1; i - h >= 0 && j - h >= 0 && board.cellState(i - h, j - h) == s; h++) {
			n++;
			moves.add(new CXCell(i - h, j - h, s));
		} // backward check
		for (int h = 1; i + h < board.M && j + h < board.N && board.cellState(i + h, j + h) == s; h++) {
			n++;
			moves.add(new CXCell(i + h, j + h, s));
		} // forward check
		if (n >= board.X) {
			moves.add(new CXCell(i, j, s));
			return moves;
		}

		// Anti-diagonal check
		n = 1;
		moves.clear();
		for (int h = 1; i - h >= 0 && j + h < board.N && board.cellState(i - h, j + h) == s; h++) {
			n++;
			moves.add(new CXCell(i - h, j + h, s));
		} // backward check
		for (int h = 1; i + h < board.M && j - h >= 0 && board.cellState(i + h, j - h) == s; h++) {
			n++;
			moves.add(new CXCell(i + h, j - h, s));
		} // forward check
		if (n >= board.X) {
			moves.add(new CXCell(i, j, s));
			return moves;
		}

		return moves;
	}

	/* draw marked cells */
	private void drawCounters(Graphics2D g2, CXCell[] list) {

		for (CXCell c : list) {

			int x = c.j * cellGap + 5;
			int y = c.i * cellGap + (Board_Top_Border + 5 + extraBorder);
			CXCellState s = c.state;
			if (s == CXCellState.P1)
				g2.setPaint(getYellowGradientPaint());
			else if (s == CXCellState.P2)
				g2.setPaint(getRedGradientPaint());
			if (!animatingCoinDrop) {
				g2.fillOval(x, y, cellGap - 10, cellGap - 10);
			} else {
				if (!(c.equals(list[list.length - 1]))) {
					g2.fillOval(x, y, cellGap - 10, cellGap - 10);
				}
			}
		}
	}

	Shape cell(int x, int y, int s) {
		GeneralPath gp = new GeneralPath();

		gp.append(new Ellipse2D.Float(x, y, s, s), false);

		gp.moveTo(x, y);
		gp.lineTo(x + s, y);
		gp.lineTo(x + s, y + s);
		gp.lineTo(x, y + s);

		gp.closePath();
		return gp;
	}

	private GradientPaint getYellowGradientPaint() {
		if (gpYellow == null)
			gpYellow = new GradientPaint(75, 75, new Color(255, 255, 0), 100, 100, new Color(200, 200, 0), true);
		return gpYellow;
	}

	private GradientPaint getRedGradientPaint() {
		if (gpRed == null)
			gpRed = new GradientPaint(75, 75, new Color(220, 0, 0), 100, 100, new Color(150, 0, 0), true);
		return gpRed;
	}

	/*
	 * shows the coin dropping to its resting point. As coin drops user input is
	 * blocked.
	 */
	private void animateCoinDrop(Graphics2D g2) {

		if (gameState == CXGameState.OPEN) {

			/*
			 * try { Thread.sleep(25); } catch (Exception e) { }
			 */

			animatingCoinDrop = (animationStartPosition - (cellGap / 2) + animationFrame < WIN_ANIMATION_FRAMES
					- cellGap * 2 + (-1 * (animationRow - NUMBER_OF_ROWS - 4)) * cellGap);

			if (animatingCoinDrop) {
				animationFrameAcceleration = 2.5 * animationFrameAcceleration;
				if (NUMBER_OF_ROWS >= 20 || NUMBER_OF_COLS >= 20) {
					animationFrame = animationFrame + NUMBER_OF_ROWS;

				}
				animationFrame = animationFrame + (int) animationFrameAcceleration + 1;

				if (board.currentPlayer == PLAYER1)
					g2.setPaint(getRedGradientPaint()); // colors here appear 'inverted' - this is because the move
				else
					g2.setPaint(getYellowGradientPaint()); // has actually been made, so animation is for last move

				// now draw the coin:
				g2.fillOval((cellGap) * animationColumn, (animationStartPosition + animationFrame - cellGap),
						cellGap - 10, cellGap - 10);

			}
		} else {
			animatingCoinDrop = false;
		}

	}

	/* draw mouse pointer on the white top border of board */
	public void drawMousePointer(Graphics2D g2) {
		// reset
		// g2.setStroke(defaultStroke);

		if (!animatingCoinDrop && Player[board.currentPlayer()] == CXPlayerType.HUMAN) {

			if (board.currentPlayer == PLAYER1)
				g2.setPaint(getYellowGradientPaint());
			if (board.currentPlayer == PLAYER2)
				g2.setPaint(getRedGradientPaint());

			if (!(isSpaceInColumn(mousePointerHorizontalPosition))) {
				g2.setPaint(Color.LIGHT_GRAY);
				fullColum = true;
			} else {
				fullColum = false;
			}

			g2.fillOval(mousePointerHorizontalPosition * cellGap + 5, mousePointerVerticalPosition - cellGap / 2,
					cellGap - 10, cellGap - 10);
			if (fullColum) {

				g2.setPaint(Color.WHITE);
				g2.setFont(new Font("Arial", Font.BOLD, 14));
				if (cellGap == 60) {
					g2.drawString("FULL!", mousePointerHorizontalPosition * cellGap + 5 + (cellGap / 10),
							mousePointerVerticalPosition);
				} else if (cellGap == 45) {
					g2.drawString("FULL!", mousePointerHorizontalPosition * cellGap + 5, mousePointerVerticalPosition);
				} else {
					g2.drawString("FULL!", mousePointerHorizontalPosition * cellGap + 5 + (cellGap / 4),
							mousePointerVerticalPosition);
				}
			}

		}
	}

	/*
	 * checks the top cell of each columns to see if it is vacant
	 */
	public boolean isSpaceInColumn(int column) {
		if (column > (NUMBER_OF_COLS - 1))
			return false;
		else
			return !board.fullColumn(column);
	}

	/*
	 * if the mouse is in the upper part of the screen a piece will be shown
	 */
	public void mouseMoved(MouseEvent mm) {
		if (Player[board.currentPlayer()] == CXPlayerType.HUMAN) {

			drawPointerOnMouse = (mm.getY() < (Board_Top_Border + extraBorder + cellGap / 1.5));
			mousePointerHorizontalPosition = ((mm.getX() / cellGap)); // drawn in center of column - not centered over
																		// the mouse
			mousePointerVerticalPosition = mm.getY(); // must be fixed place per column (no overlapping allowed).
		} else {
			drawPointerOnMouse = false;
		}
	}

	/*
	 * need to find out which column has been clicked on then post it as a move to
	 * Game.
	 */
	public void mouseClicked(MouseEvent m1) {
		if (gameState == CXGameState.OPEN) {
			if (Player[board.currentPlayer()] == CXPlayerType.HUMAN) { // Human player
				if (!animatingCoinDrop && !fullColum) {

					// only want input when no coins are currently dropping.
					int colClicked = 0;

					// large area at top for clicking to make move
					if (m1.getY() < Board_Top_Border + extraBorder + cellGap / 2) {

						colClicked = (m1.getX() / cellGap);

						if (isSpaceInColumn(colClicked))
							gameState = board.markColumn(colClicked);

						CXCell c = board.getLastMove();
						setAnimationState(true, colClicked, m1.getY(), c.i);
					}
				}
			} else { // Software player
				int curr = board.currentPlayer();
				final ExecutorService executor = Executors.newSingleThreadExecutor();
				final Future<Integer> task = executor.submit(new StoppablePlayer(ComPlayer[curr], board.copy()));
				executor.shutdown(); // Makes the ExecutorService stop accepting new tasks

				Integer c = null;

				try {
					// TIMEOUT secs + 10% more time
					c = task.get((int) (TIMEOUT + 0.1 * TIMEOUT), TimeUnit.SECONDS);
				} catch (TimeoutException ex) {
					executor.shutdownNow();
					System.err.println(ComPlayer[curr].playerName() + " interrupted due to timeout");
					System.exit(1);
				} catch (Exception ex) {
					System.err.println("Error: " + ComPlayer[curr].playerName() + " interrupted due to exception");
					System.err.println(" " + ex);
					System.exit(1);
				}
				if (!executor.isTerminated())
					executor.shutdownNow();

				if (isSpaceInColumn(c)) {
					gameState = board.markColumn(c);
					CXCell m = board.getLastMove();
					setAnimationState(true, c, Board_Top_Border + extraBorder, m.i);
				} else {
					System.err.println(ComPlayer[curr].playerName() + "  selected an illegal move!");
					System.exit(1);
				}
			}
		}
	}

	private class StoppablePlayer implements Callable<Integer> {
		private final CXPlayer P;
		private final CXBoard B;

		public StoppablePlayer(CXPlayer P, CXBoard B) {
			this.P = P;
			this.B = B;
		}

		public Integer call() throws InterruptedException {
			return P.selectColumn(B);
		}
	}

	public void mouseDragged(MouseEvent md) {
	}

	public void mouseEntered(MouseEvent m2) {
	}

	public void mouseExited(MouseEvent m3) {
	}

	public void mousePressed(MouseEvent m4) {
	}

	public void mouseReleased(MouseEvent m5) {
	}
}

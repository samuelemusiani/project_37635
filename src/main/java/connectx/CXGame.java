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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
/**
 * Initializes, updates and starts the (M,N)-game.
 * <p>Usage: CXGame M N [CXPlayer class name]</p>
 */
public class CXGame extends JFrame implements Serializable {
	/** Game Board */
	protected final CXBoard B;
	protected JLabel statusBar; // Status Bar

	JPanel mainPan;
	CXBoardPanel boardPanel; // Drawing canvas (JPanel) for the game board
	CXInputPanel inPanel; // Drawing canvas (JPanel) for east panel

	public enum CXGameType {
		HUMANvsHUMAN, HUMANvsCOMPUTER, COMPUTERvsCOMPUTER
	}

	protected enum CXPlayerType {
		HUMAN, COMPUTER
	}

	protected CXPlayerType[] Player = new CXPlayerType[2];
	protected CXGameType gameType;
	protected final int TIMEOUT = 10; // 10 seconds timeout

	CXGameState gameState; 

	protected static CXPlayer[] ComPlayer = new CXPlayer[2];

	private static int cell_size; // cell width and height
	private final static int EXTRA_VERTICAL_BORDER = 136; //width inPanel
	public final static int EXTRA_ORIZONTAL_BORDER = 150; //height for white top bar and status bar

	private static final long serialVersionUID = 1L; // seriaisable 
	private int data;

	public static CXGame game;

	public CXGame(int M, int N, int X, CXGameType type) {

		B = new CXBoard(M, N, X);
		gameState = B.gameState();
		gameType = type;

		mainPan = new JPanel();

		// Setup the status bar (JLabel) to display status message
		statusBar = new JLabel("  ");
		statusBar.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 15));
		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 4, 5));

		boardPanel = new CXBoardPanel(B, type, (new ProgBorder()).getProgBorder(), cell_size, ComPlayer, Player,statusBar);
		inPanel = new CXInputPanel((new ProgBorder()).getProgBorder(), boardPanel);

		mainPan.setLayout(new BorderLayout(5, 5));
		mainPan.add(boardPanel, BorderLayout.CENTER);
		mainPan.add(inPanel, BorderLayout.EAST);
		mainPan.add(statusBar, BorderLayout.PAGE_END);
        
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
 		cp.add(mainPan, BorderLayout.CENTER);
        
		setSize(cell_size * N  + EXTRA_VERTICAL_BORDER, cell_size * M + EXTRA_ORIZONTAL_BORDER);
		setResizable(false);
		setLocation(100, 50);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		initGame(); // initialize board and variables
	}

	public void selectPlayerTurn() {
		if (Player[0] == null) {
			if (gameType == CXGameType.HUMANvsHUMAN) {
				Player[0] = CXPlayerType.HUMAN;
				Player[1] = CXPlayerType.HUMAN;
			} else if (gameType == CXGameType.HUMANvsCOMPUTER) {
				Player[0] = CXPlayerType.COMPUTER;
				Player[1] = CXPlayerType.HUMAN;
			} else if (gameType == CXGameType.COMPUTERvsCOMPUTER) {
				Player[0] = CXPlayerType.COMPUTER;
				Player[1] = CXPlayerType.COMPUTER;
			}
		} else { // from second game, switch
			CXPlayerType tmp1 = Player[0];
			Player[0] = Player[1];
			Player[1] = tmp1;
			CXPlayer tmp2 = ComPlayer[0];
			ComPlayer[0] = ComPlayer[1];
			ComPlayer[1] = tmp2;
		}
	}

	public void initGame() {
		selectPlayerTurn();
		// Timed-out initialization of the CXPlayer
		if (gameType != CXGameType.HUMANvsHUMAN) {
			for (int k = 0; k < 2; k++) {
				final int i = k; // need to have a final variable here
				if (ComPlayer[i] != null) {
					final Runnable initPlayer = new Thread() {
						@Override
						public void run() {
							ComPlayer[i].initPlayer(B.M, B.N, B.X, i == 0, TIMEOUT);
						}
					};

					final ExecutorService executor = Executors.newSingleThreadExecutor();
					final Future<?> future = executor.submit(initPlayer);
					executor.shutdown();
					try {
						// TIMEOUT secs + 10% more time
						future.get((int) (TIMEOUT + 0.1 * TIMEOUT), TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						System.err.println("Error: " + ComPlayer[i].playerName() + " interrupted: initialization takes too much time");
						System.exit(1);
					} catch (Exception e) {
						System.err.println(e);
						System.exit(1);
					}
					if (!executor.isTerminated())
						executor.shutdownNow();
				}
			}
		}

		B.reset();

		String P1 = Player[0] == CXPlayerType.HUMAN ? "Human" : ComPlayer[0].playerName();
		String P2 = Player[1] == CXPlayerType.HUMAN ? "Human" : ComPlayer[1].playerName();
		setTitle("Connect" + B.X + " - "  + P1 + " vs " + P2);

	}

	public void SerializeMe(int data) {
		this.data = data;
	}

	public int getData() {
		return data;
	}

	public static void main(String[] args) {
		if (args.length != 3 && args.length != 4 && args.length != 5) {
			System.err.println("Usage: CXGame <M> <N> <X> [CXPlayer class] [CXPlayer class]");
			System.exit(0);
		}

		// Size of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = screenSize.height - EXTRA_ORIZONTAL_BORDER; // screen height minus some space for top and bottom bar
		int screenWidth = screenSize.width;

		int M = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int X = Integer.parseInt(args[2]);

		// Parameters check
		if (M <= 0 || N <= 0 || X <= 0) {
			System.err.println("Error: M, N, k must be larger than 0");
			System.exit(1);
		}

		cell_size = 90;
		
		// Select the cell_size according to M, N and screen size
		if ((screenHeight / M) < cell_size) {
			cell_size = 60;
			System.err.println("Auto change cell_size, cell_size = " + cell_size);
		}
		if ((screenHeight / M) < cell_size) {
			cell_size = 45;
			System.err.println("Auto change cell_size, cell_size = " + cell_size);
		}
		if (screenHeight / M < cell_size) {
			System.err.println("Error: M = " + M + " is too large for the screen dimensions. Max allowed value: " + (screenHeight / cell_size));
			System.exit(1);
		}
		if ((screenWidth / N) < cell_size) {
			cell_size = 60;
			System.err.println("Auto change cell_size, cell_size = " + cell_size);
		}
		if ((screenWidth / N) < cell_size) {
			cell_size = 45;
			System.err.println("Auto change cell_size, cell_size = " + cell_size);
		}
		if (screenWidth / N < cell_size) {
			System.err.println("Error: N = " + N + " is too large for the screen dimensions. Max allowed value: " + (screenWidth / cell_size));
			System.exit(1);
		}

		// Check if the class parameter exists and it is an CXPlayer implementation
		if (args.length == 4 || args.length == 5) {
			try {
				ComPlayer[0] = (CXPlayer) Class.forName(args[3]).getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException e) {
				System.err.println("Error: \'" + args[3] + "\' class not found");
				System.exit(1);
			} catch (ClassCastException e) {
				System.err.println("Error: \'" + args[3] + "\' class does not implement the CXPlayer interface");
				System.exit(1);
			} catch (NoSuchMethodException e) {
				System.err.println("Error: \'" + args[3] + "\' class constructor needs to be empty");
				System.exit(1);
			} catch (Exception e) {
				System.err.println("  " + e);
				System.exit(1);
			}
		}

		// Check if the class parameter exists and it is an CXPlayer implementation
		if (args.length == 5) {
			try {
				ComPlayer[1] = (CXPlayer) Class.forName(args[4]).getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException e) {
				System.err.println("Error: \'" + args[4] + "\' class not found");
				System.exit(1);
			} catch (ClassCastException e) {
				System.err.println("Error: \'" + args[4] + "\' class does not implement the CXPlayer interface");
				System.exit(1);
			} catch (NoSuchMethodException e) {
				System.err.println("Error: \'" + args[4] + "\' class constructor needs to be empty");
				System.exit(1);
			} catch (Exception e) {
				System.err.println("  " + e);
				System.exit(1);
			}
		}
		// Select the game type
		CXGameType type = args.length == 5 ? CXGameType.COMPUTERvsCOMPUTER : args.length == 4 ? CXGameType.HUMANvsCOMPUTER : CXGameType.HUMANvsHUMAN;

		game = new CXGame(M, N, X, type);
		game.setVisible(true);  // show this JFrame
	}
	
	public class ProgBorder {
		Border progBord;

		public ProgBorder() {
			Border inner = new CompoundBorder(BorderFactory.createLineBorder(Color.black),BorderFactory.createEmptyBorder(5, 8, 5, 8));
			Border outer = new CompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5));
			progBord     = new CompoundBorder(outer, inner);
		}

		public Border getProgBorder() {
			return progBord;
		}
	}
}

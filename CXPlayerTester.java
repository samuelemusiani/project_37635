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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs a game against two CXPlayer classes and prints the game scores:
 * <ul>
 * <li>3 if the player wins</li>
 * <li>1 if the game ends in draw</li>
 * <li>0 if the player loses
 * </ul>
 * <p>
 * Usage: CXPlayerTester [OPTIONS] M N X CXPlayer class name; CXPlayer class
 * name;<br>
 * OPTIONS:<br>
 * -t timeout&gt; Timeout in seconds</br>
 * -r rounds; Number of rounds</br>
 * -g graphic; Graphic terminal board -v Verbose
 * </p>
 */
public class CXPlayerTester {
	private static int TIMEOUT = 10;
	private static int ROUNDS = 1;
	private static boolean VERBOSE = false;

	private static int M;
	private static int N;
	private static int X;

	private static CXBoard B;

	private static CXPlayer[] Player = new CXPlayer[2];

	/** Scoring system */
	private static int WINSCORE  = 3;
	private static int DRAWSCORE = 1;
	private static int ERRSCORE  = 3;

	private enum GameState {
		WINP1, WINP2, DRAW, ERRP1, ERRP2, EP1EX, EP2EX;
	}

	private CXPlayerTester() {
	}

	private static void initGame() {
		if (VERBOSE)
			System.out.println("Initializing " + M + "," + N + " board");
		B = new CXBoard(M, N, X);
		// Timed-out initialization of the CXPlayers
		for (int k = 0; k < 2; k++) {
			if (VERBOSE)
				if (VERBOSE)
					System.out.println("Initializing " + Player[k].playerName() + " as Player " + (k + 1));
			final int i = k; // need to have a final variable here
			final Runnable initPlayer = new Thread() {
				@Override
				public void run() {
					Player[i].initPlayer(B.M, B.N, B.X, i == 0, TIMEOUT);
				}
			};

			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future<?> future = executor.submit(initPlayer);
			executor.shutdown();
			try {
				future.get(TIMEOUT, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				System.err.println(
						"Error: " + Player[i].playerName() + " interrupted: initialization takes too much time");
				System.exit(1);
			} catch (Exception e) {
				System.err.println(e);
				System.exit(1);
			}
			if (!executor.isTerminated())
				executor.shutdownNow();
		}
		if (VERBOSE)
			System.out.println();
	}

	private static class StoppablePlayer implements Callable<Integer> {
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

	private static GameState runGame() {
		while (B.gameState() == CXGameState.OPEN) {
			int curr = B.currentPlayer();
			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Future<Integer> task     = executor.submit(new StoppablePlayer(Player[curr], B.copy()));
			executor.shutdown(); // Makes the ExecutorService stop accepting new tasks

			Integer c = null;

			try {
				c = task.get(TIMEOUT, TimeUnit.SECONDS);
			} catch (TimeoutException ex) {
				int n = 3; // Wait some more time to see if it stops
				System.err.println(
						"Player " + (curr + 1) + " (" + Player[curr].playerName() + ") interrupted due to timeout");
				while (!task.isDone() && n > 0) {
					System.err.println("Waiting for " + Player[curr].playerName() + " to stop ... (" + n + ")");
					try {
						Thread.sleep(TIMEOUT * 1000);
					} catch (InterruptedException e) {
					}
					n--;
				}

				if (n == 0) {
					System.err.println(
							"Player " + (curr + 1) + " (" + Player[curr].playerName() + ") still running: game closed");
					return curr == 0 ? GameState.EP1EX : GameState.EP2EX;
				} else {
					System.err.println("Player " + (curr + 1) + " (" + Player[curr].playerName()
							+ ") eventually stopped: round closed");
					return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
				}
			} catch (Exception ex) {
				int n = 3; // Wait some more time to see if it stops
				System.err.println(
						"Player " + (curr + 1) + " (" + Player[curr].playerName() + ") interrupted due to exception");
				System.err.println(" " + ex);
				while (!task.isDone() && n > 0) {
					System.err.println("Waiting for " + Player[curr].playerName() + " to stop ... (" + n + ")");
					try {
						Thread.sleep(TIMEOUT * 1000);
					} catch (InterruptedException e) {
					}
					n--;
				}
				if (n == 0) {
					System.err.println(
							"Player " + (curr + 1) + " (" + Player[curr].playerName() + ") still running: game closed");
					return curr == 0 ? GameState.EP1EX : GameState.EP2EX;
				} else {
					System.err.println("Player " + (curr + 1) + " (" + Player[curr].playerName()
							+ ") eventually stopped: round closed");
					return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
				}
			}

			if (!executor.isTerminated())
				executor.shutdownNow();

			try {
				int r = 0;
				B.markColumn(c);
				if (VERBOSE) {
					for (int i = 0; i < B.M; i++) {
						if (B.cellState(i, c) != CXCellState.FREE) {
							r = i;
							break;
						}
					}
					System.out.println(
							"Player " + (curr + 1) + " (" + Player[curr].playerName() + ") -> [" + r + "," + c + "]");
				}
			} catch (Exception ex) {
				System.err.println("Player " + (curr + 1) + " (" + Player[curr].playerName()
						+ ")  selected an illegal move [" + c + "]: round closed");
				return curr == 0 ? GameState.ERRP1 : GameState.ERRP2;
			}
		}

		return B.gameState() == CXGameState.DRAW ? GameState.DRAW
				: (B.gameState() == CXGameState.WINP1 ? GameState.WINP1 : GameState.WINP2);
	}

	private static void parseArgs(String args[]) {
		List<String> L = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(0)) {
			case '-':
				char c = (args[i].length() != 2 ? 'x' : args[i].charAt(1));
				switch (c) {
				case 't':
					if (args.length < i + 2)
						throw new IllegalArgumentException("Expected parameter after " + args[i]);

					try {
						TIMEOUT = Integer.parseInt(args[++i]);
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"Illegal integer format for " + args[i - 1] + " argument: " + args[i]);
					}
					break;
				case 'r':
					if (args.length < i + 2)
						throw new IllegalArgumentException("Expected parameter after " + args[i]);

					try {
						ROUNDS = Integer.parseInt(args[++i]);
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"Illegal integer format for " + args[i - 1] + " argument: " + args[i]);
					}
					break;
				case 'v':
					VERBOSE = true;
					break;
				default:
					throw new IllegalArgumentException("Illegal argument:  " + args[i]);
				}
				break;
			default:
				L.add(args[i]);
			}
		}

		int n = L.size();
		if (n != 5)
			throw new IllegalArgumentException("Missing arguments:" + (n < 1 ? " <M>" : "") + (n < 2 ? " <N>" : "")
					+ (n < 3 ? " <X>" : "") + (n < 4 ? " <MNKPlayer class>" : "") + (n < 5 ? " <MNKPlayer class>" : ""));

		try {
			M = Integer.parseInt(L.get(0));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for M argument: " + M);
		}
		try {
			N = Integer.parseInt(L.get(1));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for N argument: " + N);
		}
		try {
			X  = Integer.parseInt(L.get(2));
		}
		catch(NumberFormatException e) {
			throw new IllegalArgumentException("Illegal integer format for N argument: " + X);
		}

		if (M <= 0 || N <= 0 || X <= 0)
			throw new IllegalArgumentException("Arguments  M, N, X must be larger than 0");

		String[] P = { L.get(3), L.get(4) };
		for (int i = 0; i < 2; i++) {
			try {
				Player[i] = (CXPlayer) Class.forName(P[i]).getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Illegal argument: \'" + P[i] + "\' class not found");
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(
						"Illegal argument: \'" + P[i] + "\' class does not implement the CXPlayer interface");
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
						"Illegal argument: \'" + P[i] + "\' class constructor needs to be empty");
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Illegal argument: \'" + P[i] + "\' class (unexpected exception) " + e);
			}
		}
	}

	private static void printUsage() {
		System.err.println("Usage: CXPlayerTester [OPTIONS] <M> <N> <X> <CXPlayer class> <CXPlayer class>");
		System.err.println("OPTIONS:");
		System.err.println("  -t <timeout>  Timeout in seconds. Default: " + TIMEOUT);
		System.err.println("  -r <rounds>   Number of rounds. Default: " + ROUNDS);
		System.err.println("  -v            Verbose. Default: " + VERBOSE);
	}

	public static void main(String[] args) {
		int P1SCORE  = 0;
		int P2SCORE  = 0;
		int[] STATP1 = new int[3];      
    int[] STATP2 = new int[3];

		if (args.length == 0) {
			printUsage();
			System.exit(0);
		}

		try {
			parseArgs(args);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}

		if (VERBOSE) {
			System.out.println("Game type : " + M + "," + N + "," + X);
			System.out.println("Player1   : " + Player[0].playerName());
			System.out.println("Player2   : " + Player[1].playerName());
			System.out.println("Rounds    : " + ROUNDS);
			System.out.println("Timeout   : " + TIMEOUT + " secs\n\n");
		}

		boolean stop = false; 
		for (int i = 1; i <= ROUNDS && !stop; i++) {
			if (VERBOSE)
				System.out.println("\n**** ROUND " + i + " ****");
			initGame();
			GameState state = runGame();

			switch (state) {
			case WINP1:
				P1SCORE += WINSCORE;  STATP1[0]++;
				break;
			case WINP2:
				P2SCORE += WINSCORE;  STATP2[0]++;
				break;
			case ERRP1:
				P2SCORE += ERRSCORE;  STATP1[2]++;
				break;
			case ERRP2:
				P1SCORE += ERRSCORE;  STATP2[2]++;
				break;
			case EP1EX:
				P2SCORE += ERRSCORE;  STATP1[2]++; stop=true; 
				break;
			case EP2EX:
				P1SCORE += ERRSCORE;  STATP2[2]++; stop=true;
				break;
			case DRAW:
				P1SCORE += DRAWSCORE; STATP1[1]++;
				P2SCORE += DRAWSCORE; STATP2[1]++;
				break;
			}
			if (VERBOSE) {
				System.out.println("\nGame state    : " + state);
				System.out.println("Current score : " + Player[0].playerName() + " (" + P1SCORE + ") - "
						+ Player[1].playerName() + " (" + P2SCORE + ")");
			}
		}
		if (VERBOSE)
			System.out.println("\n**** FINAL SCORE ****");
		System.out.println(Player[0].playerName() + " Score: " + P1SCORE + " Won: " + STATP1[0] + " Lost: " + STATP2[0] + " Draw: " + STATP1[1] + " Error: " + STATP1[2]);
		System.out.println(Player[1].playerName() + " Score: " + P2SCORE + " Won: " + STATP2[0] + " Lost: " + STATP1[0] + " Draw: " + STATP2[1] + " Error: " + STATP2[2]); 
		
		System.exit(0);
	}

}

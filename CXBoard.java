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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;
//import java.util.stream.Collectors;

/**
 * Board for an (M,N)-game.
 * <p>
 * The CXBoard class allows only alternates moves between two players. It
 * mantains the ordered list of moves and allows undoes.
 * </p>
 */

public class CXBoard {
	/**
	 * Board rows
	 */
	public final int M;

	/**
	 * Board columns
	 */
	public final int N;

	/**
	 * Number of symbols to be aligned (horizontally, vertically, diagonally) for a  win
	 */
	public final int X;

	// grid for the board
	protected CXCellState[][] B;

	protected LinkedList<CXCell> MC;   // Marked Cells stack (used to undo)
	protected int                RP[]; // First free row position
	protected TreeSet<Integer>   AC;   // Availabe (not full) columns
	
	// we define characters for players (PR for Red, PY for Yellow)
	private final CXCellState[] Player = {CXCellState.P1, CXCellState.P2};

	protected int currentPlayer; // currentPlayer plays next move

	protected CXGameState gameState; // game state


	/**
	 * Create a board of size MxN and initialize the game parameters
	 * 
	 * @param M Board rows
	 * @param N Board columns
   * @param X Number of symbols to be aligned (horizontally, vertically, diagonally) for a win
	 *
	 * @throws IllegalArgumentException If M,N are smaller than 1
	 */
	public CXBoard(int M, int N, int X) throws IllegalArgumentException {
		if (M <= 0)
			throw new IllegalArgumentException("M cannot be smaller than 1");
		if (N <= 0)
			throw new IllegalArgumentException("N cannot be smaller than 1");
		if (X <= 0)
			throw new IllegalArgumentException("X cannot be smaller than 1");

		this.M = M;
		this.N = N;
		this.X = X;

		B  = new CXCellState[M][N];
		MC = new LinkedList<CXCell>();
		RP = new int[N];
		AC = new TreeSet<Integer>();
		reset();

	}

	/**
	 * Resets the CXBoard
	 */
	public void reset() {
		currentPlayer = 0;
		gameState     = CXGameState.OPEN;
		initBoard();
		initDataStructures();
	}

	// Sets to free all board cells
	private void initBoard() {
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				B[i][j] = CXCellState.FREE;
	}

	//Resets the marked cells list and other data structures
	private void initDataStructures() {
		this.MC.clear();
		this.AC.clear();
		for (int j = 0; j < N; j++) {
			RP[j] = M-1;
			AC.add(j);
		}
	}

	/**
	 * Returns the state of cell <code>i,j</code>
	 *
	 * @param i i-th row
	 * @param j j-th column
	 *
	 * @return State of the <code>i,j</code> cell (FREE,P1,P2)
	 * @throws IndexOutOfBoundsException If <code>i,j</code> are out of matrix bounds
	 * 
	 */
	public CXCellState cellState(int i, int j) throws IndexOutOfBoundsException {
		if (i < 0 || i >= M || j < 0 || j >= N)
			throw new IndexOutOfBoundsException("Indexes " + i + "," + j + " are out of matrix bounds");
		else
			return B[i][j];
	}

	/**
   * Check whether a column is full 
   * 
   * @param col column number
	 * 
	 * @return true if col is outside matrix bounds of if it is full  
   */
	public boolean fullColumn(int col) {
		return  col < 0 || col >= N || RP[col] == -1; 
	}

	/**
	 * Retrieves the last move
	 *
	 * @return CXCell object or null
	 */
	public CXCell getLastMove() {
		if (MC.size() == 0) 
			return null;
		else 
			return MC.peekLast();
	}

	/**
	 * Returns the current state of the game.
	 *
	 * @return CXGameState enumeration constant (OPEN,WINP1,WINP2,DRAW)
	 */
	public CXGameState gameState() {
		return gameState;
	}

	/**
	 * Returns the id of the player allowed to play next move.
	 *
	 * @return 0 (first player) or 1 (second player)
	 */
	public int currentPlayer() {
		return currentPlayer;
	}

	/**
	 * Returns the number of free cells in the game board.
	 *
	 * @return number of free cells
	 */
	public int numOfFreeCells() {
		return M*N-MC.size();
	}
	
	/**
	 * Returns the number of marked cells in the game board.
	 *
	 * @return number of marked cells
	 */
	public int numOfMarkedCells() {
		return MC.size();
	}

	/**
	 * Mark the first free cell on the selected column
	 * 
	 * @return CXGameState (OPEN,WINP1,WINP2,DRAW)
	 */
	public CXGameState markColumn(int col) throws IndexOutOfBoundsException, IllegalStateException {
		if (gameState != CXGameState.OPEN) { // Game already ended
			throw new IllegalStateException("Game ended!");
		} else if (!(0 <= col && col < N)) { // Column index out of matrix bounds
			throw new IndexOutOfBoundsException("Index " + col + " out of matrix bounds\n" + "Column must be between 0 and " + (N - 1));
		} else if (RP[col] == -1) {          // Column full
			throw new IllegalStateException("Column " + col + " is full.");
		} else {
			int row = RP[col]--;
			if (RP[col] == -1) AC.remove(col);
			B[row][col] = Player[currentPlayer];
			CXCell newc = new CXCell(row, col, Player[currentPlayer]);
			MC.add(newc); // Add move to the history

			currentPlayer = (currentPlayer + 1) % 2;

			if (isWinningMove(row, col))
				gameState = B[row][col] == CXCellState.P1 ? CXGameState.WINP1 : CXGameState.WINP2;
			else if (MC.size() == M * N)
				gameState = CXGameState.DRAW;

			return gameState;
		}
	}

	/**
	 * Undo last move
	 *
	 * @throws IllegalStateException If there is no move to undo
	 */
	public void unmarkColumn() throws IllegalStateException {
		if (MC.size() == 0) {
			throw new IllegalStateException("No move to undo");
		} else {
			CXCell oldc = MC.removeLast();

			B[oldc.i][oldc.j] = CXCellState.FREE;
			RP[oldc.j]++;
			if(RP[oldc.j] == 0) AC.add(oldc.j); 

			currentPlayer = (currentPlayer + 1) % 2;
			gameState = CXGameState.OPEN;
		}
	}

	/**
	 * Returns the marked cells list in array format.
	 * <p>
	 * This is the history of the game: the first move is in the array head, the
	 * last move in the array tail.
	 * </p>
	 * 
	 * @return List of marked cells
	 */
	public CXCell[] getMarkedCells() {
		return MC.toArray(new CXCell[MC.size()]);
	}

	/**
   * Returns the list of still available columns in array format.
   * <p>
   * This is the list of still playable columns in the matrix.
   * </p>
   * 
   * @return List of available column indexes 
   */
  public Integer[] getAvailableColumns() {
		return AC.toArray(new Integer[AC.size()]);
  }

	/**
   * Returns a copy of the main board
   *
   * @return An MxN matrix of cell statest
   */
	public CXCellState[][] getBoard() {
		CXCellState[][] C = new CXCellState[M][N];

		for(int i = 0; i < M; i++)
			for(int j = 0; j < N; j++)
				C[i][j] = B[i][j];		

		return C;
	}

	/**
	 * Returns a copy of the CXBoard object
	 *
	 * @return A CXBoard
	 */
	public CXBoard copy() {
		CXBoard C = new CXBoard(M,N,X);
		for(CXCell c : this.getMarkedCells())
			C.markColumn(c.j);
		return C;
  }


	// Check winning state from cell i, j
	private boolean isWinningMove(int i, int j) {
		CXCellState s = B[i][j];
		int n;

		// Useless pedantic check
		if (s == CXCellState.FREE)
			return false;

		// Horizontal check
		n = 1;
		for (int k = 1; j-k >= 0 && B[i][j-k] == s; k++) n++; // backward check
		for (int k = 1; j+k <  N && B[i][j+k] == s; k++) n++; // forward check
		if (n >= X) return true;

		// Vertical check
		n = 1;
		for (int k = 1; i+k <  M && B[i+k][j] == s; k++) n++;
		if (n >= X) return true;

		// Diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j-k >= 0 && B[i-k][j-k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j+k <  N && B[i+k][j+k] == s; k++) n++; // forward check
		if (n >= X) return true;

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j+k <  N && B[i-k][j+k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j-k >= 0 && B[i+k][j-k] == s; k++) n++; // forward check
		if (n >= X) return true;

		return false;
	}
}

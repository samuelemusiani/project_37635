package connectx.Players;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Stack;

public class CXBitBoard {
  // Rows
  public final int Rows;

  // Columns
  public final int Columns;

  // Number of symbols to be aligned for a win
  public final int ToAlign;

  // grid for the board
  long position;
  long mask;

  protected Stack<Integer> MC; // Marked Cells stack (used to undo)
  protected int RP[]; // First free row position
  protected TreeSet<Integer> AC; // Availabe (not full) columns
  //
  // // we define characters for players (PR for Red, PY for Yellow)
  // private final CXCellState[] Player = { CXCellState.P1, CXCellState.P2 };

  protected boolean currentPlayer; // currentPlayer plays next move false -> 0, true -> 1

  protected int gameState; // game state:
                           // 1 win player 0,
                           // 0 draw,
                           // -1 win payer 1
                           // 2 OPEN

  public CXBitBoard(int M, int N, int X) {
    this.Rows = M;
    this.Columns = N;
    this.ToAlign = X;

    MC = new Stack<Integer>();
    RP = new int[N];
    AC = new TreeSet<Integer>();
    // reset();
  }

  /**
   * Resets the CXBoard
   */
  public void reset() {
    currentPlayer = false; // Player 0
    gameState = 2; // Open
    initBoard();
    initDataStructures();
  }

  // Sets to free all board cells
  private void initBoard() {
    position = 0;
    mask = 0;
  }

  // Resets the marked cells list and other data structures
  private void initDataStructures() {
    this.MC.clear();
    this.AC.clear();
    for (int j = 0; j < Columns; j++) {
      RP[j] = 0;
      AC.add(j);
    }
  }

  // 1 -> Player 1, 2 -> player 2, 0 empty
  public int cellState(int i, int j) {
    int tmp = (1 << (Rows * i + j));

    if ((position & tmp) != 0) {
      return currentPlayer ? 2 : 1;
    } else if ((mask & tmp) != 0) {
      return currentPlayer ? 1 : 2;
    } else
      return 0;
  }

  public boolean fullColumn(int col) {
    return (mask & (1 << ((Rows + 1) * col + Rows - 1))) == 0;
  }

  public int getLastMove() {
    return MC.pop();
  }

  public int gameState() {
    return gameState;
  }

  /**
   * Returns the id of the player allowed to play next move.
   *
   * @return 0 (first player) or 1 (second player)
   */
  public int currentPlayer() {
    return currentPlayer ? 1 : 0;
  }

  /**
   * Returns the number of free cells in the game board.
   *
   * @return number of free cells
   */
  public int numOfFreeCells() {
    return Rows * Columns - MC.size();
  }

  /**
   * Returns the number of marked cells in the game board.
   *
   * @return number of marked cells
   */
  public int numOfMarkedCells() {
    return MC.size();
  }

  // game state:
  // 1 win player 0,
  // 0 draw,
  // -1 win payer 1
  // 2 OPEN
  public int markColumn(int col) {
    int row = RP[col]++;
    if (RP[col] == Rows)
      AC.remove(col);

    long new_position = position ^ mask;
    long new_mask = mask | (mask + (1 << (col * (Rows + 1))));

    position = new_position;
    mask = new_mask;

    // B[row][col] = Player[currentPlayer];
    // CXCell newc = new CXCell(row, col, Player[currentPlayer]);
    MC.add(col); // Add move to the history

    currentPlayer = !currentPlayer;

    if (isWinningMove())
      gameState = !currentPlayer ? -1 : 1;
    else if (MC.size() == Rows * Columns)
      gameState = 0;

    return gameState;
  }

  /**
   * Undo last move
   */
  public void unmarkColumn() {
    Integer col = MC.pop();

    long new_mask = mask ^ (1 << ((Rows + 1) * col + RP[col] - 1));
    long new_position = position ^ new_mask;

    position = new_position;
    mask = new_mask;

    // [oldc.i][oldc.j] = CXCellState.FREE;
    // [oldc.j]++;
    // (RP[oldc.j] == 0)
    // .add(oldc.j);

    currentPlayer = !currentPlayer;
    gameState = 2; // Open
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

  // Check winning state from cell i, j
  private boolean isWinningMove() {

    // Horizontal check
    int rowsInc = Rows + 1;
    long m = position & (position >> rowsInc);
    m = m & (m >> (rowsInc * 2));

    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (position & (m >> rowsInc)) != 0) {
      return true;
    }

    // Vertical check
    m = position & (position >> 1);
    m = m & (m >> 2);
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (position & (m >> 1)) != 0) {
      return true;
    }

    // Diagonal \
    m = position & (position >> Rows);
    m = m & (m >> (Rows * 2));
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (position & (m >> Rows)) != 0) {
      return true;
    }

    // Diagonal /
    rowsInc = Rows + 2;
    m = position & (position >> rowsInc);
    m = m & (m >> (rowsInc * 2));
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (position & (m >> rowsInc)) != 0) {
      return true;
    }

    return false;
  }
}

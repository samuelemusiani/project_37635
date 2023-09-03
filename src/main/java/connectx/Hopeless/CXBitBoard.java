package connectx.Hopeless;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Stack;
import connectx.CXCell;
import connectx.CXCellState;

public class CXBitBoard {
  // Rows
  public final int Rows;

  // Columns
  public final int Columns;

  // Number of symbols to be aligned for a win
  public final int ToAlign;

  // grid for the board
  private long position;
  private long mask;

  protected Stack<Integer> MC; // Marked Cells stack (used to undo)
  protected int RP[]; // First free row position
  protected TreeSet<Integer> AC; // Availabe (not full) columns

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
    reset();
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
    long tmp = (1l << ((Rows + 1) * j + i));

    if ((position & tmp) != 0) {
      return currentPlayer ? 2 : 1;
    } else if ((mask & tmp) != 0) {
      return currentPlayer ? 1 : 2;
    } else
      return 0;
  }

  public boolean fullColumn(int col) {
    return RP[col] == Rows;
  }

  public CXCell getLastMove() {
    int tmp = MC.peek();

    return new CXCell(RP[tmp] - 1, tmp, currentPlayer ? CXCellState.P1 : CXCellState.P2);
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
  public void markColumn(int col) {
    RP[col]++;
    if (RP[col] == Rows)
      AC.remove(col);

    position = position ^ mask;
    long tmp_mask = mask | (mask + (1l << (col * (Rows + 1))));
    mask = tmp_mask;

    MC.add(col); // Add move to the history

    currentPlayer = !currentPlayer;

    if (isWinningMove())
      gameState = !currentPlayer ? -1 : 1;
    else if (MC.size() == Rows * Columns)
      gameState = 0;
  }

  /**
   * Undo last move
   */
  public void unmarkColumn() {
    Integer col = MC.pop();
    RP[col]--;
    if (RP[col] == Rows - 1)
      AC.add(col);

    mask = mask ^ (1l << ((Rows + 1) * col + RP[col]));
    position = position ^ mask;

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

  private boolean isWinningMove() {
    long tmp_position = this.position ^ mask; // Redefinition

    // Horizontal check
    int rowsInc = Rows + 1;
    long m = tmp_position & (tmp_position << rowsInc);
    m = m & (m << (rowsInc * 2));

    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (tmp_position & (m << rowsInc)) != 0) {
      return true;
    }

    // Vertical check
    m = tmp_position & (tmp_position << 1);
    m = m & (m << 2);
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (tmp_position & (m << 1)) != 0) {
      return true;
    }

    // Diagonal \
    m = tmp_position & (tmp_position << Rows);
    m = m & (m << (Rows * 2));
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (tmp_position & (m << Rows)) != 0) {
      return true;
    }

    // Diagonal /
    rowsInc = Rows + 2;
    m = tmp_position & (tmp_position << rowsInc);
    m = m & (m << (rowsInc * 2));
    if (ToAlign == 4 && m != 0) {
      return true;
    } else if (ToAlign == 5 && (tmp_position & (m << rowsInc)) != 0) {
      return true;
    }

    return false;
  }

  public void setPosition(long position) {
    this.position = position;
  }

  public void setMask(long mask) {
    this.mask = mask;
  }

  public CXBitBoard copy() {
    CXBitBoard C = new CXBitBoard(Rows, Columns, ToAlign);
    for (int i : MC) {
      C.markColumn(i);
    }
    return C;
  }

  public long hash() {
    return position + mask;
  }
}

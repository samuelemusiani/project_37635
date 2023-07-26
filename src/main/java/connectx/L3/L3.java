package connectx.L3;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/*
 * NegaMax implementation
 * The algoriths search all the game tree until he founds a winning move or
 * runs out of time. No memorization is used, so every time the full tree is 
 * scanned.
 */
public class L3 implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  public L3() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
  }

  /**
   * Selects a free colum on game board.
   * <p>
   * Selects a winning column (if any). If the program run out of time
   * selects a random column.
   * </p>
   */
  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis(); // Save starting time

    Integer[] L = B.getAvailableColumns();
    int save = L[rand.nextInt(L.length)]; // Save a random column

    try {
      return move_maxi(B, 20);
    } catch (TimeoutException e) {
      System.err.println("Timeout!!! Random column selected");
      return save;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  private int move_maxi(CXBoard B, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      int max = -1000000;
      Integer[] moves = B.getAvailableColumns();
      int move = moves[0];

      for (int i : moves) {
        B.markColumn(i);
        int score = mini(B, depth - 1);
        B.unmarkColumn();
        if (score > max) {
          max = score;
          move = i;
        }
      }
      return move;
    } else
      throw new TimeoutException();
  }


  private int maxi(CXBoard B, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      int max = -1000000;
      if (depth == 0) return max;

      Integer[] moves = B.getAvailableColumns();

      for (int i : moves) {
        B.markColumn(i);
        int score = mini(B, depth - 1);
        B.unmarkColumn();
        if (score > max)
          max = score;
      }
      return max;
    } else if (B.gameState() == myWin)
      return 1;
    else if (B.gameState() == yourWin)
      return -1;
    else
      return 0;
  }

  private int mini(CXBoard B, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      int min = 1000000;
      if (depth == 0) return min;

      Integer[] moves = B.getAvailableColumns();

      for (int i : moves) {
        B.markColumn(i);
        int score = maxi(B, depth - 1);
        B.unmarkColumn();
        if (score < min)
          min = score;
      }
      return min;
    } else if (B.gameState() == myWin)
      return 1;
    else if (B.gameState() == yourWin)
      return -1;
    else
      return 0;
  }

  public String playerName() {
    return "L3";
  }
}

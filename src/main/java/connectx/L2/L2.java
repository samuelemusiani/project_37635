package connectx.L2;

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
public class L2 implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  public L2() {
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
      return move_negaMax(B);
    } catch (TimeoutException e) {
      System.err.println("Timeout!!! Random column selected");
      return save;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  private int move_negaMax(CXBoard B) throws TimeoutException {
    // Is the first call of the nega_max which returna move instead of the score
    if (B.gameState() == CXGameState.OPEN) {
      Integer[] moves = B.getAvailableColumns();
      int max = -10000000; // -INFINITY
      int move = 0;

      for (int i : moves) {
        B.markColumn(i);
        int score = -negaMax(B, -1);
        if (score > max) {
          max = score;
          move = i;

          if (max == 1) {
            B.unmarkColumn();
            break;
          }
        }
        B.unmarkColumn();
      }
      // System.out.println(max + " " + last_move);
      return move;
    } else
      throw new TimeoutException(); // Ugly but should do the job
  }

  private int negaMax(CXBoard B, int color) throws TimeoutException {
    checktime();
    // switch (B.gameState()) {
    // case OPEN:
    // System.out.println("Open");
    // break;
    // case WINP1:
    // System.out.println("WINP1");
    // break;
    // case WINP2:
    // System.out.println("WINP2");
    // break;
    // case DRAW:
    // System.out.println("DRAW");
    // break;
    // }
    if (B.gameState() == CXGameState.OPEN) {
      Integer[] moves = B.getAvailableColumns();
      int max = -10000000; // -INFINITY

      for (int i : moves) {
        B.markColumn(i);
        int score = -negaMax(B, color == 1 ? -1 : 1);
        if (score > max) {
          max = score;

          if (max == 1) {
            B.unmarkColumn();
            break;
          }
        }
        B.unmarkColumn();
      }
      // System.out.println(max + " " + last_move);
      return max;
    } else if (B.gameState() == myWin)
      return 1 * color;
    else if (B.gameState() == yourWin)
      return -1 * color;
    else // Draw
      return 0;
  }

  public String playerName() {
    return "L2";
  }
}

package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/*
 * AlphaBeta implementation
 */
public class L4 implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  public L4() {
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
      return move_maxi(B, 10000);
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
      int alpha = -1000000000;
      int beta = -alpha;
      Integer[] moves = B.getAvailableColumns();
      int move = moves[0];

      for (int i : moves) {
        B.markColumn(i);
        int score = alphaBetaMin(B, alpha, beta, depth - 1);
        // System.out.println("Score: " + score);
        B.unmarkColumn();
        if (score > alpha) {
          alpha = score;
          // System.out.println("Max_int: " + max);
          move = i;
        }
      }
      // System.out.println("Max: " + max);
      return move;
    } else
      throw new TimeoutException();
  }

  private int alphaBetaMax(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      if (depth == 0)
        return alpha;

      Integer[] moves = B.getAvailableColumns();

      for (int i : moves) {
        B.markColumn(i);
        int score = alphaBetaMin(B, alpha, beta, depth - 1);
        B.unmarkColumn();
        if (score >= beta)
          return beta;

        if (score > alpha)
          alpha = score;
      }
      return alpha;
    } else
      return evaluate(B);
  }

  private int alphaBetaMin(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      if (depth == 0)
        return beta;

      Integer[] moves = B.getAvailableColumns();

      for (int i : moves) {
        B.markColumn(i);
        int score = alphaBetaMax(B, alpha, beta, depth - 1);
        B.unmarkColumn();
        if (score <= alpha)
          return alpha;

        if (score < beta)
          beta = score;
      }
      return beta;
    } else
      return evaluate(B);
  }

  private int evaluate(CXBoard B) {
    if (B.gameState() == myWin)
      return 1;
    else if (B.gameState() == yourWin)
      return -1;
    else
      return 0;
  }

  public String playerName() {
    return "L4";
  }
}

package connectx.L5;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/*
 * AlphaBeta implementation with different evaluation for the final position
 */
public class L5 implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  public L5() {
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

    Integer[] L = reorderMoves(B.getAvailableColumns());
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
      int alpha = (int) -(1.5 * B.numOfFreeCells());
      int beta = -alpha;
      Integer[] moves = reorderMoves(B.getAvailableColumns()); 
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
      // System.out.println("Move: " + move);
      return move;
    } else
      throw new TimeoutException();
  }

  private int alphaBetaMax(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      if (depth == 0)
        return alpha;

      Integer[] moves = reorderMoves(B.getAvailableColumns()); 

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
      return (B.numOfFreeCells() + 1) / 2 + 5; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 5;
    else
      return 0;
  }

  private Integer[] reorderMoves(Integer[] m) {
    int l = m.length;
    Integer[] r = new Integer[l];

    int delta = 0;

    for (int i = 0; i < l; i++) {
      if (i % 2 == 1)
        delta++;

      r[i] = m[l / 2 + (i % 2 == 0 ? 1 : -1) * delta];
    }

    return r;
  }

  public String playerName() {
    return "L5";
  }
}

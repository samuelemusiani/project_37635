package connectx.L6;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/*
 * L5 + transposition table
 */
public class L6 implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private HashMap<String, Integer> table; // Converto to array of char??
  private String moves_made;

  public L6() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;

    table = new HashMap<String, Integer>();
    moves_made = "";
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
    // int save = L[rand.nextInt(L.length)]; // Save a random column
    int save = L[0];

    CXCell last_move = B.getLastMove();
    if (last_move != null)
      moves_made += last_move.j;

    try {
      int move = move_maxi(B, moves_made, 10000000);
      moves_made += move;
      return move;
    } catch (TimeoutException e) {
      System.err.println("Timeout!!! Random column selected");
      moves_made += save;
      return save;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  private int move_maxi(CXBoard B, String moves, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      int alpha = (int) -(1.5 * B.numOfFreeCells());
      int beta = -alpha;
      Integer[] possible_moves = reorderMoves(B.getAvailableColumns());
      // System.err.println("Possible_moves:" + Arrays.toString(possible_moves));
      int move = possible_moves[0];

      for (int i : possible_moves) {
        B.markColumn(i);
        moves += i;

        // System.err.println("Move: " + i);

        Integer score = table.get(moves);
        if (score == null) {
          // System.err.println("[" + alpha + ", " + beta + "]");
          score = alphaBetaMin(B, alpha, beta, moves, depth - 1);
        }

        if (score > alpha) {
          alpha = score;
          table.put(moves, score);
          move = i;
        }
        B.unmarkColumn();
        moves = moves.substring(0, moves.length() - 1); // I don't like this
        // System.err.println("Score: " + score);
      }
      // System.err.println("Alpha: " + alpha);
      return move;
    } else
      throw new TimeoutException();
  }

  private int alphaBetaMax(CXBoard B, int alpha, int beta, String moves, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      // if (depth == 0)
      // return alpha;

      Integer[] possible_moves = reorderMoves(B.getAvailableColumns());

      for (int i : possible_moves) {
        B.markColumn(i);
        moves += i;
        Integer score = table.get(moves);
        if ( score == null) {
          score = alphaBetaMin(B, alpha, beta, moves, depth - 1);
        }

        if (score >= beta) {
          B.unmarkColumn();
          moves = moves.substring(0, moves.length() - 1); // I don't like this
          return beta;
        }

        if (score > alpha) {
          alpha = score;
          table.put(moves, score);
        }
        B.unmarkColumn();
        moves = moves.substring(0, moves.length() - 1); // I don't like this
      }
      return alpha;
    } else
      return evaluate(B);
  }

  private int alphaBetaMin(CXBoard B, int alpha, int beta, String moves, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      // if (depth == 0)
      // return beta;

      Integer[] possible_moves = B.getAvailableColumns();

      for (int i : possible_moves) {
        B.markColumn(i);
        moves += i;
        int score = alphaBetaMax(B, alpha, beta, moves, depth - 1);
        B.unmarkColumn();
        moves = moves.substring(0, moves.length() - 1); // I don't like this
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
    return "L6";
  }
}

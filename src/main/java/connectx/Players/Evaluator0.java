package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.lang.Math;

/*
 * Copy of L7 made to test the heuristic evaluate() function
 */
public class Evaluator0 implements CXPlayer {
  private CXGameState myWin;
  private CXGameState yourWin;
  private boolean am_i_fist;
  private int TIMEOUT;
  private long START;
  private HashMap<Long, Integer> table; // Converto to array of char??

  private int evaPositionalMatrix[][];

  // Heuristic tester
  private int divergence;
  private int count_heuristic;

  public Evaluator0() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
    am_i_fist = first;

    table = new HashMap<Long, Integer>();

    evaPositionalMatrix = new int[M][N];
    int max_dist = (int) Math.sqrt(M * M + N * N) / 2;
    int center_x = N / 2;
    int center_y = M / 2;
    for (int i = 0; i < M; i++) {
      for (int j = N / 2; j < N; j++) {
        evaPositionalMatrix[i][j] = (int) (max_dist - Math.sqrt(Math.pow(j - center_x, 2) + Math.pow(i - center_y, 2)));
      }
    }

    for (int i = 0; i < M; i++) {
      for (int j = 0; j < N / 2; j++) {
        evaPositionalMatrix[i][j] = evaPositionalMatrix[i][N - j - 1];
      }
    }

    for (int i = 0; i < M; i++) {
      System.err.println(Arrays.toString(evaPositionalMatrix[i]));
    }

    divergence = 0;
    count_heuristic = 0;
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
    int save = L[0];

    try {
      // System.err.println("Position: " + convertPosition(B));
      int move = move_maxi(B, 8);
      System.err.println("Heuristic divergence:" + (double) divergence / count_heuristic);
      return move;
    } catch (TimeoutException e) {
      System.err.println("Timeout!!! Random column selected");
      return save;
    }
  }

  private void printTable(CXBoard B) {
    for (int i = 0; i < B.M; i++) {
      for (int j = 0; j < B.N; j++) {
        switch (B.cellState(i, j)) {
          case FREE:
            System.err.print("0 ");
            break;

          case P1:
            System.err.print("1 ");
            break;

          case P2:
            System.err.print("2 ");
            break;
        }
      }
      System.err.println();
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
      Integer[] possible_moves = reorderMoves(B.getAvailableColumns());
      // System.err.println("Possible_moves:" + Arrays.toString(possible_moves));
      int move = possible_moves[0];

      for (int i : possible_moves) {
        B.markColumn(i);
        long converted_position = convertPosition(B);

        // System.err.println("Move: " + i);

        Integer score = table.get(converted_position);
        if (score == null) {
          // System.err.println("[" + alpha + ", " + beta + "]");
          score = alphaBetaMin(B, alpha, beta, depth);
          System.err.println("Score: " + score);
          int eval = evaluate(B);
          System.err.println("Evaluation: " + eval);
          divergence += Math.abs(score - eval);
          count_heuristic++;

          if (Math.abs(score - eval) > 4)
            printTable(B);
        }

        if (score > alpha) {
          alpha = score;
          table.put(converted_position, score);
          move = i;
        }
        B.unmarkColumn();
        // System.err.println("Score: " + score);
      }
      // System.err.println("Alpha: " + alpha);
      return move;
    } else
      throw new TimeoutException();
  }

  private int alphaBetaMax(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      if (depth <= 0)
        return evaluate(B);

      Integer[] possible_moves = reorderMoves(B.getAvailableColumns());

      for (int i : possible_moves) {
        B.markColumn(i);
        long converted_position = convertPosition(B);
        Integer score = table.get(converted_position);
        if (score == null) {
          score = alphaBetaMin(B, alpha, beta, depth);
        }

        if (score >= beta) {
          B.unmarkColumn();
          return beta;
        }

        if (score > alpha) {
          alpha = score;
          table.put(converted_position, score);
        }
        B.unmarkColumn();
      }
      return alpha;
    } else
      return evaluate_win(B);
  }

  private int alphaBetaMin(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {

      Integer[] possible_moves = B.getAvailableColumns();

      for (int i : possible_moves) {
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
      return evaluate_win(B);
  }

  private int evaluate(CXBoard B) {
    int sum = 0;

    // Check the position of my pieces and opponent pieces
    // The more neare the center the more point 1 piece gets
    for (int i = 0; i < B.M; i++) {
      for (int j = 0; j < B.N; j++) {
        switch (B.cellState(B.M - i - 1, j)) {
          case P1:
            sum += am_i_fist ? evaPositionalMatrix[i][j] : -evaPositionalMatrix[i][j];
            break;

          case P2:
            sum += am_i_fist ? -evaPositionalMatrix[i][j] : evaPositionalMatrix[i][j];
            break;

          case FREE:
            break;
        }
      }
    }

    // Need to check the adjacent pieces

    // Horizzontal
    for (int i = 0; i < B.M; i++) {
      boolean player_analized = false; // flase -> 1, true -> 2
      int count = 0;
      boolean is_there_a_space = false;

      for (int j = 0; j < B.N; j++) {

        switch (B.cellState(B.M - i - 1, j)) {
          case P1:
            if (player_analized == false)
              count++;
            else {
              if (is_there_a_space)
                count = 1; // Use count for a possible move in the space
              else
                sum += (Math.pow(1.7, count) - 1) *
                    (player_analized != am_i_fist ? 1 : -1);
            }
            player_analized = false;
            is_there_a_space = false;
            break;

          case P2:
            if (player_analized == true)
              count++;
            else {
              if (is_there_a_space)
                count = 1; // Use count for a possible move in the space
              else
                sum += (Math.pow(1.7, count) - 1) *
                    (player_analized != am_i_fist ? 1 : -1);
            }
            player_analized = true;
            is_there_a_space = false;
            break;

          case FREE:
            sum += (Math.pow(2, count) - 1) *
                (player_analized != am_i_fist ? 1 : -1);
            is_there_a_space = true;
            count = 0;
            break;
        }
      }
    }

    // Vertical
    for (int j = 0; j < B.N; j++) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;
      for (int i = B.M - 1; i >= 0 && !exit; i--) {
        switch (B.cellState(B.M - i - 1, j)) {
          case P1:
            if (!found_player) {
              found_player = true;
              player = false;
              count++;
            } else {
              if (player == false)
                count++;
              else
                exit = true;
            }
            break;

          case P2:
            if (!found_player) {
              found_player = true;
              player = true;
            } else {
              if (player == true)
                count++;
              else
                exit = true;
            }
            break;

          case FREE:
            count_free += 1;
            break;
        }
      }
      if (found_player) {
        int tmp = (int) count * count / 2;
        if (count + count_free < B.X)
          tmp /= 3;
        sum += tmp * (player != am_i_fist ? 1 : -1);
      }
    }

    // Diagonal check??

    return sum;
  }

  private int evaluate_win(CXBoard B) {
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

  private long convertPosition(CXBoard B) {
    // M -> Righe
    // N -> Colonne
    // For the board rappresentation:
    // http://blog.gamesolver.org/solving-connect-four/06-bitboard/

    long position = 0;
    long mask = 0;
    long bottom = 0;

    for (int j = B.N - 1; j >= 0; j--) {
      for (int i = B.M - 1; i >= 0; i--) {
        position = position << 1;
        mask = mask << 1;
        bottom = bottom << 1;
        switch (B.cellState(B.M - i - 1, j)) {
          case P1:
            // System.err.println("INDICI: " + i + " " + j);
            position |= 1;
            mask |= 1;
            break;

          case P2:
            mask |= 1;
            break;

          case FREE:
            break;
        }
      }
      bottom |= 1;
    }
    // System.err.println("Pos: " + position);
    // System.err.println("Mask: " + mask);
    // System.err.println("Bottom: " + bottom);
    return position + mask + bottom;
  }

  public String playerName() {
    return "Evaluator0";
  }
}

package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/*
 * L7 + bassic euristic  + iterative deepening and move reordering from previous iteration
 */
public class L8 implements CXPlayer {
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  // Transposition table
  private HashMap<Long, Integer> table; 
  private HashMap<Long, Integer> table_depth; 

  // Heuristic
  private int evaPositionalMatrix[][];
  private boolean am_i_fist;


  // Iterative deepening
  private int current_best_move;
  private boolean search_not_finished;
  private int previous_search_depth;

  public L8() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    evaPositionalMatrix = new int[M][N];
    int max_dist = (int) Math.sqrt(M * M + N * N) / 2;
    int center_x = N / 2;
    int center_y = M / 2;
    for (int i = 0; i < M; i++) {
      for (int j = N / 2; j < N; j++) {
        evaPositionalMatrix[i][j] = 10 + (int) (max_dist - Math.sqrt(Math.pow(j - center_x, 2) + Math.pow(i - center_y, 2)));
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

    Integer[] L = reorderMoves(B);
    current_best_move = L[0];

    try {
      // System.err.println("Position: " + convertPosition(B));
      return iterativeDeepening(B); // Keeps going untile a TimeoutException
    } catch (TimeoutException e) {
      // System.err.println("Timeout!!! Random column selected");
      System.err.println("Timeout! Fall back on previous best move");
      return current_best_move;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  private int iterativeDeepening(CXBoard B) throws TimeoutException {
    int depth = Math.max(previous_search_depth - 1, 1);
    search_not_finished = true;
    while (search_not_finished) {
      System.err.println("Depth: " + depth);
      search_not_finished = false;
      current_best_move = move_maxi(B, depth);
      // System.err.println("Current_best_move: " + current_best_move);
      previous_search_depth = depth;
      depth++;
      search_not_finished = depth < 3 * B.numOfFreeCells();
    }
    return current_best_move;
  }

  private int move_maxi(CXBoard B, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() == CXGameState.OPEN) {
      int alpha = (int) -(1.5 * B.numOfFreeCells());
      int beta = -alpha;
      Integer[] possible_moves = reorderMoves(B);
      // System.err.println("Possible_moves:" + Arrays.toString(possible_moves));
      int move = possible_moves[0];

      for (int i : possible_moves) {
        B.markColumn(i);
        long converted_position = convertPosition(B);

        // System.err.println("Move: " + i);

        Integer score = table.get(converted_position);
        Integer score_depth = table_depth.get(converted_position);
        if (score == null || score_depth < depth) {
          // System.err.println("[" + alpha + ", " + beta + "]");
          score = alphaBetaMin(B, alpha, beta, depth - 1);
          // System.err.println("Depth interna: " + depth);
        }

        if (score > alpha) {
          alpha = score;
          table.put(converted_position, score);
          table_depth.put(converted_position, depth);
          move = i;
        }
        B.unmarkColumn();
        // System.err.println("Move: " + i);
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
      if (depth <= 0) {
        search_not_finished = true;
        return evaluate(B);
      }

      Integer[] possible_moves = reorderMoves(B);

      for (int i : possible_moves) {
        B.markColumn(i);
        long converted_position = convertPosition(B);
        Integer score = table.get(converted_position);
        Integer score_depth = table_depth.get(converted_position);
        if (score == null || score_depth < depth) {
          score = alphaBetaMin(B, alpha, beta, depth - 1);
        }

        if (score >= beta) {
          B.unmarkColumn();
          return beta;
        }

        if (score > alpha) {
          alpha = score;
          table.put(converted_position, score);
          table_depth.put(converted_position, depth);
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
      if (depth <= 0) {
        search_not_finished = true;
        return evaluate(B);
      }

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
      return (B.numOfFreeCells() + 1) / 2 + 1000000; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 1000000;
    else
      return 0;
  }

  private class intPair {
    int first;
    int second;

    public intPair() {
      first = 0;
      second = 0;
    }
  }

  private Integer[] reorderMoves(CXBoard B) {
    Integer[] m = B.getAvailableColumns();
    int l = m.length;

    intPair[] tmp = new intPair[l];
    for (int i = 0; i < l; i++) {
      B.markColumn(m[i]);
      Integer val = table.get(convertPosition(B));
      tmp[i] = new intPair();
      tmp[i].first = val == null ? 0 : val;
      tmp[i].second = m[i];
      B.unmarkColumn();
    }
    
    // O(N^2) selection sort :)
    for (int i = 0; i < l - 1; i++) {
      int min = i;
      for (int j = i + 1; j < l; j++) {
        if (tmp[j].first < tmp[min].first)
          min = j;
      }

      if (min != i) {
        intPair pair_tmp = new intPair();

        pair_tmp.first = tmp[min].first;
        pair_tmp.second = tmp[min].second;

        tmp[min].first = tmp[i].first;
        tmp[min].second = tmp[i].second;

        tmp[i].first = pair_tmp.first; 
        tmp[i].second = pair_tmp.second;
      }
    }

    Integer[] r = new Integer[l];

    for (int i = 0; i < l; i++) {
      r[i] = tmp[i].second;
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
    return "L8";
  }
}

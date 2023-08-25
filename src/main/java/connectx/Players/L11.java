package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.Players.CXBitBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/*
 * l9 copy with small boards bitwise optimization 
 * (can't play large board for now)
 */
public class L11 implements CXPlayer {
  private int myWin;
  private int yourWin;
  private int TIMEOUT;
  private long START;

  private int rows;
  private int columns;

  CXBitBoard board;

  // Transposition table
  private HashMap<Long, Integer> table;
  private HashMap<Long, Integer> table_depth;

  private boolean isBoardTooBig;
  private long[][] zobrist_table;
  private int[] static_column_fullnes;
  private int[] column_fullnes;
  long current_position;

  // Heuristic
  private int evaPositionalMatrix[][];
  private boolean am_i_fist;

  // Iterative deepening
  private int current_best_move;
  private boolean search_not_finished;
  private int previous_search_depth;

  // Tmp
  private int fullSearches;
  private int fastSearches;
  private int tableHits;
  private int tableMiss;

  private int evaluateCalls;

  public L11() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    myWin = first ? 1 : -1;
    yourWin = first ? -1 : 1;
    TIMEOUT = timeout_in_secs;

    rows = M;
    columns = N;
    current_position = 0;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    isBoardTooBig = (M + 1) * N > 64;

    if (!isBoardTooBig) {
      System.err.println("Board SMALL");
      board = new CXBitBoard(M, N, K);
    } else {
      System.err.println("Board BIG");
      Random rand = new Random(System.currentTimeMillis());

      zobrist_table = new long[2 * M][2 * N];

      for (int j = 0; j < 2 * M; j++) {
        for (int i = 0; i < 2 * N; i++) {
          zobrist_table[j][i] = rand.nextInt();
        }
      }

      for (int j = 0; j < 2 * M; j++) {
        System.err.println(Arrays.toString(zobrist_table[j]));
      }

      column_fullnes = new int[N];
      static_column_fullnes = new int[N];

      for (int i = 0; i < N; i++) {
        column_fullnes[i] = 0;
        static_column_fullnes[i] = 0;
      }
    }

    evaPositionalMatrix = new int[M][N];
    int max_dist = (int) Math.sqrt(M * M + N * N) / 2;
    int center_x = N / 2;
    int center_y = M / 2;
    for (int i = 0; i < M; i++) {
      for (int j = N / 2; j < N; j++) {
        evaPositionalMatrix[i][j] = 10
            + (int) (2 * (max_dist - Math.sqrt(Math.pow(j - center_x, 2) + Math.pow(i - center_y, 2))));
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

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis(); // Save starting time

    if (B.numOfMarkedCells() > 0) {
      int c = B.getLastMove().j;
      board.markColumn(c);
    }

    Integer[] L = reorderMoves(board);
    current_best_move = L[0];

    if (table.size() > 2_000_000) // Avoid heap errors
      table.clear();

    board.printMoves();

    try {
      int move = iterativeDeepening(board.copy());
      System.err.println("Move: " + move);
      System.err.println("EvaluateCalls: " + evaluateCalls);
      board.markColumn(move);

      return move;
    } catch (TimeoutException e) {
      // System.err.println("Timeout!!! Random column selected");
      System.err.println("EvaluateCalls: " + evaluateCalls);
      System.err.println("Timeout! Fall back on previous best move");

      System.err.println("Move: " + current_best_move);
      board.markColumn(current_best_move);

      return current_best_move;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (98.0 / 100.0))
      throw new TimeoutException();
  }

  private int iterativeDeepening(CXBitBoard B) throws TimeoutException {
    int depth = 2;
    if (am_i_fist)
      depth = Math.max(previous_search_depth - 2, 2);
    else
      depth = Math.max(previous_search_depth - 2, 1);

    tableHits = 0;
    tableMiss = 0;

    evaluateCalls = 0;

    search_not_finished = true;
    while (search_not_finished) {
      fullSearches = 0;
      fastSearches = 0;
      System.err.println("Depth: " + depth);
      search_not_finished = false;
      current_best_move = move_pvSearch(B, depth);
      // System.err.println("Current_best_move: " + current_best_move);
      System.err.println("FULL searches: " + fullSearches);
      System.err.println("FAST searches: " + fastSearches);
      System.err.println("Table HITS: " + tableHits);
      System.err.println("Table MISS: " + tableMiss);
      System.err.println("Table ENTRIES: " + table.size());
      previous_search_depth = depth;
      search_not_finished = depth < Math.max(2 * B.numOfFreeCells(), 8);
      depth += 2;
      // break;
    }
    previous_search_depth += 2;

    return current_best_move;
  }

  private int move_pvSearch(CXBitBoard B, int depth) throws TimeoutException {
    checktime();
    int alpha = Integer.MIN_VALUE + 5;
    int beta = Integer.MAX_VALUE - 5;
    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);
    int move = possible_moves[0];

    for (int i : possible_moves) {
      B.markColumn(i);

      // long converted_position = convertPosition(B);
      long converted_position = B.hash();

      // System.err.println("CONVERSION: " + (converted_position == position));

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, true);
        } else {
          fastSearches++;
          score = -zwSearch(B, -alpha, depth - 1, true);
          if (score > alpha /* && score < beta */) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, true);
          }
        }
      } else
        tableHits++;

      System.err.println("Score: " + score);
      table.put(converted_position, score);
      table_depth.put(converted_position, depth);
      B.unmarkColumn();

      if (score > alpha) {
        alpha = score;
        move = i;
        System.err.println("Entered");
      }
      bSearchPv = false;
    }

    System.err.println("Alpha: " + alpha);
    return move;
  }

  private int pvSearch(CXBitBoard B, int alpha, int beta, int depth,
      boolean whoIsPlaying) throws TimeoutException {
    checktime();
    if (B.gameState() != 2)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);

    for (int i : possible_moves) {
      B.markColumn(i);

      // long converted_position = convertPosition(B);
      long converted_position = B.hash();

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying);
        } else {
          fastSearches++;
          score = -zwSearch(B, -alpha, depth - 1, !whoIsPlaying);
          if (score > alpha /* && score < beta */) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying);
          }
        }
      } else
        tableHits++;

      table.put(converted_position, score);
      table_depth.put(converted_position, depth);

      B.unmarkColumn();

      if (score >= beta)
        return beta;

      if (score > alpha) {
        alpha = score;
      }
      bSearchPv = false;
    }

    return alpha;
  }

  // fail-hard zero window search, returns either beta-1 or beta
  private int zwSearch(CXBitBoard B, int beta, int depth,
      boolean whoIsPlaying) throws TimeoutException {
    checktime();
    if (B.gameState() != 2)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    Integer[] possible_moves = reorderMoves(B);
    for (int i : possible_moves) {
      B.markColumn(i);
      int score = -zwSearch(B, 1 - beta, depth - 1, !whoIsPlaying);
      B.unmarkColumn();

      if (score >= beta)
        return beta;
    }
    return beta - 1;
  }

  private int evaluate(CXBitBoard B) {
    evaluateCalls++;
    int sum = 0;

    // Check the position of my pieces and opponent pieces
    // The more neare the center the more point 1 piece gets
    for (int i = 0; i < B.Rows; i++) {
      for (int j = 0; j < B.Columns; j++) {
        // switch (B.cellState(B.Rows - i - 1, j)) {
        switch (B.cellState(i, j)) {
          case 1:
            sum += am_i_fist ? evaPositionalMatrix[i][j] : -evaPositionalMatrix[i][j];
            break;

          case 2:
            sum += am_i_fist ? -evaPositionalMatrix[i][j] : evaPositionalMatrix[i][j];
            break;

          case 0:
            break;
        }
      }
    }

    // Need to check the adjacent pieces

    // Horizzontal
    for (int i = 0; i < B.Rows; i++) {
      boolean player_analized = false; // flase -> 1, true -> 2
      int count = 0;
      boolean is_there_a_space = false;

      for (int j = 0; j < B.Columns; j++) {

        // switch (B.cellState(B.Rows - i - 1, j)) {
        switch (B.cellState(i, j)) {
          case 1:
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

          case 2:
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

          case 0:
            sum += (Math.pow(2, count) - 1) *
                (player_analized != am_i_fist ? 1 : -1);
            is_there_a_space = true;
            count = 0;
            break;
        }
      }
    }

    // Vertical
    for (int j = 0; j < B.Columns; j++) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;
      for (int i = B.Rows - 1; i >= 0 && !exit; i--) {
        // switch (B.cellState(B.Rows - i - 1, j)) {
        switch (B.cellState(i, j)) {
          case 1:
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

          case 2:
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

          case 0:
            count_free += 1;
            break;
        }
      }
      if (found_player) {
        int tmp = (int) count * count / 2;
        if (count + count_free < B.ToAlign)
          tmp /= 3;
        sum += tmp * (player != am_i_fist ? 1 : -1);
      }
    }

    // Diagonal check??

    return sum;
  }

  private int evaluate_win(CXBitBoard B) {
    evaluateCalls++;
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + 1000000000; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 1000000000;
    else
      return 0;
  }

  private Integer[] reorderMoves(CXBitBoard B) {
    Integer[] m = B.getAvailableColumns();
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
    if (!isBoardTooBig) {
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
    } else {
      // Need to hash large boards

      long sum = 0;

      for (int j = 0; j < B.M; j++) {
        for (int i = 0; i < B.N; i++) {
          switch (B.cellState(B.M - j - 1, i)) {
            case P1:
              sum ^= zobrist_table[j][i];
              break;

            case P2:
              sum ^= zobrist_table[rows + j][columns + i];
              break;

            case FREE:
              break;
          }
        }
      }
      // System.err.println("Zobrist: " + sum);
      return sum;
    }
  }

  private long zobristMakeMove(long pos, int i, boolean whoHasPlayed) {
    if (!isBoardTooBig)
      return pos;

    // System.err.println("column_fullnes[i]: " + Arrays.toString(column_fullnes));

    // System.err.println("ZobristMAKEmove: " + i);
    // System.err.println("whoHasPlayed: " + whoHasPlayed);

    if (whoHasPlayed) {
      // System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      // System.err.println("rows: " + rows);
      // System.err.println("i + columns: " + (i + columns));
      pos ^= zobrist_table[column_fullnes[i] + rows][i + columns];
    } else {
      // System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      pos ^= zobrist_table[column_fullnes[i]][i];
    }

    // System.err.println("Made the move");

    column_fullnes[i]++;
    return pos;
  }

  private long zobristUnmakeMove(long pos, int i, boolean whoHasPlayed) {
    if (!isBoardTooBig)
      return pos;

    // System.err.println("ZobristUNMAKEmove: " + i);
    // System.err.println("whoHasPlayed: " + whoHasPlayed);

    column_fullnes[i]--;
    if (whoHasPlayed) {
      // System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      // System.err.println("rows: " + rows);
      // System.err.println("i + columns: " + (i + columns));
      pos ^= zobrist_table[column_fullnes[i] + rows][i + columns];
    } else {
      // System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      pos ^= zobrist_table[column_fullnes[i]][i];
    }
    return pos;
  }

  public String playerName() {
    return "L11";
  }
}

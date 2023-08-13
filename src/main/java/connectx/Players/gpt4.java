package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/*
 * L9 copy with gpt4 heuristic
 */
public class gpt4 implements CXPlayer {
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  private int rows;
  private int columns;

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

  public gpt4() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;

    rows = M;
    columns = N;
    current_position = 0;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    isBoardTooBig = (M + 1) * N > 64;

    if (!isBoardTooBig)
      System.err.println("Board SMALL");
    else {
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

    Integer[] L = reorderMoves(B);
    current_best_move = L[0];

    try {
      if (isBoardTooBig) {
        for (int i = 0; i < columns; i++) {
          column_fullnes[i] = static_column_fullnes[i];
        }

        if (B.numOfMarkedCells() > 0) {
          int c = B.getLastMove().j;
          current_position = zobristMakeMove(current_position, c, true);
          static_column_fullnes[c]++;
        }
      }

      int move = iterativeDeepening(B, current_position);

      if (isBoardTooBig) {
        column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
        current_position = zobristMakeMove(current_position, move, false);
        static_column_fullnes[move]++;
      }
      return move;
    } catch (TimeoutException e) {
      // System.err.println("Timeout!!! Random column selected");
      System.err.println("Timeout! Fall back on previous best move");
      if (isBoardTooBig) {
        column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
        current_position = zobristMakeMove(current_position, current_best_move,
            false);

        static_column_fullnes[current_best_move]++;
      }

      return current_best_move;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  private int iterativeDeepening(CXBoard B, long position) throws TimeoutException {
    int depth;
    if (am_i_fist)
      depth = Math.max(previous_search_depth - 2, 2);
    else
      depth = Math.max(previous_search_depth - 2, 1);

    tableHits = 0;
    tableMiss = 0;

    search_not_finished = true;
    while (search_not_finished) {
      fullSearches = 0;
      fastSearches = 0;
      System.err.println("Depth: " + depth);
      search_not_finished = false;
      current_best_move = move_pvSearch(B, depth, position);
      // System.err.println("Current_best_move: " + current_best_move);
      System.err.println("FULL searches: " + fullSearches);
      System.err.println("FAST searches: " + fastSearches);
      System.err.println("Table HITS: " + tableHits);
      System.err.println("Table MISS: " + tableMiss);
      System.err.println("Table ENTRIES: " + table.size());
      previous_search_depth = depth;
      search_not_finished = depth < Math.max(2 * B.numOfFreeCells(), 8);
      depth += 2;
    }
    previous_search_depth += 2;

    return current_best_move;
  }

  private int move_pvSearch(CXBoard B, int depth, long position) throws TimeoutException {
    checktime();
    int alpha = Integer.MIN_VALUE + 5;
    int beta = Integer.MAX_VALUE - 5;
    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);
    int move = possible_moves[0];

    for (int i : possible_moves) {
      B.markColumn(i);
      position = zobristMakeMove(position, i, false);

      // For now avoid transposition

      long converted_position = convertPosition(B);

      // System.err.println("CONVERSION: " + (converted_position == position));

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
        } else {
          fastSearches++;
          score = -zwSearch(B, -alpha, depth - 1, true, position);
          if (score > alpha /* && score < beta */) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
          }
        }
      } else
        tableHits++;

      table.put(converted_position, score);
      table_depth.put(converted_position, depth);
      B.unmarkColumn();
      position = zobristUnmakeMove(position, i, false);

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

  private int pvSearch(CXBoard B, int alpha, int beta, int depth,
      boolean whoIsPlaying, long position) throws TimeoutException {
    checktime();
    if (B.gameState() != CXGameState.OPEN)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);

    for (int i : possible_moves) {
      B.markColumn(i);
      position = zobristMakeMove(position, i, whoIsPlaying);

      // For now avoid transposition

      long converted_position = convertPosition(B);
      // System.err.println("CONVERSION: " + (converted_position == position));

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
        } else {
          fastSearches++;
          score = -zwSearch(B, -alpha, depth - 1, !whoIsPlaying, position);
          if (score > alpha /* && score < beta */) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
          }
        }
      } else
        tableHits++;

      table.put(converted_position, score);
      table_depth.put(converted_position, depth);

      B.unmarkColumn();
      position = zobristUnmakeMove(position, i, whoIsPlaying);

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
  private int zwSearch(CXBoard B, int beta, int depth,
      boolean whoIsPlaying, long position) throws TimeoutException {
    checktime();
    if (B.gameState() != CXGameState.OPEN)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    Integer[] possible_moves = reorderMoves(B);
    for (int i : possible_moves) {
      B.markColumn(i);
      int score = -zwSearch(B, 1 - beta, depth - 1, !whoIsPlaying, position);
      B.unmarkColumn();

      if (score >= beta)
        return beta;
    }
    return beta - 1;
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
    //
    // Diagonal GPT4
    //

    for (int i = 0; i < B.M; i++) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;
      for (int j = 0; j < B.N && i + j < B.M && !exit; j++) {
        switch (B.cellState(B.M - i - j - 1, j)) {
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

    for (int j = 1; j < B.N; j++) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;
      for (int i = 0; i < B.M && i + j < B.N && !exit; i++) {
        switch (B.cellState(B.M - i - 1, j + i)) {
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

    for (int i = 0; i < B.M; i++) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;
      for (int j = 0; j < B.N && i - j >= 0 && !exit; j++) {
        switch (B.cellState(B.M - i + j - 1, j)) {
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

    for (int j = B.N - 2; j >= 0; j--) {
      int count = 0;
      int count_free = 0;
      boolean found_player = false;
      boolean player = false;
      boolean exit = false;

      for (int i = 0; i < B.M && j - i >= 0 && !exit; i++) {
        switch (B.cellState(B.M - i - 1, j - i)) {
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

    return sum;
  }

  private int evaluate_win(CXBoard B) {
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + 1000000000; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 1000000000;
    else
      return 0;
  }

  // private class intPair {
  // int first;
  // int second;
  //
  // public intPair() {
  // first = 0;
  // second = 0;
  // }
  // }

  private Integer[] reorderMoves(CXBoard B) {
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

    // This code should reorder moves based on the transposition
    // table value, but it perform worst and slower than the simple
    // move ordering where the center is preferred.
    // I think this i due to the heuristic, but for now I should keep
    // the simple reordering

    // Integer[] m = B.getAvailableColumns();
    // int l = m.length;
    //
    // intPair[] tmp = new intPair[l];
    // for (int i = 0; i < l; i++) {
    // B.markColumn(m[i]);
    // Integer val = table.get(convertPosition(B));
    // tmp[i] = new intPair();
    // tmp[i].first = val == null ? 0 : val;
    // tmp[i].second = m[i];
    // B.unmarkColumn();
    // }
    //
    // // O(N^2) selection sort :)
    // for (int i = 0; i < l - 1; i++) {
    // int min = i;
    // for (int j = i + 1; j < l; j++) {
    // if (tmp[j].first < tmp[min].first)
    // min = j;
    // }
    //
    // if (min != i) {
    // intPair pair_tmp = new intPair();
    //
    // pair_tmp.first = tmp[min].first;
    // pair_tmp.second = tmp[min].second;
    //
    // tmp[min].first = tmp[i].first;
    // tmp[min].second = tmp[i].second;
    //
    // tmp[i].first = pair_tmp.first;
    // tmp[i].second = pair_tmp.second;
    // }
    // }
    //
    // Integer[] r = new Integer[l];
    //
    // for (int i = 0; i < l; i++) {
    // r[i] = tmp[i].second;
    // }
    // return r;
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

    System.err.println("ZobristMAKEmove: " + i);
    System.err.println("whoHasPlayed: " + whoHasPlayed);

    if (whoHasPlayed) {
      System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      System.err.println("rows: " + rows);
      System.err.println("i + columns: " + (i + columns));
      pos ^= zobrist_table[column_fullnes[i] + rows][i + columns];
    } else {
      System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      pos ^= zobrist_table[column_fullnes[i]][i];
    }

    System.err.println("Made the move");

    column_fullnes[i]++;
    return pos;
  }

  private long zobristUnmakeMove(long pos, int i, boolean whoHasPlayed) {
    if (!isBoardTooBig)
      return pos;

    System.err.println("ZobristUNMAKEmove: " + i);
    System.err.println("whoHasPlayed: " + whoHasPlayed);

    column_fullnes[i]--;
    if (whoHasPlayed) {
      System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      System.err.println("rows: " + rows);
      System.err.println("i + columns: " + (i + columns));
      pos ^= zobrist_table[column_fullnes[i] + rows][i + columns];
    } else {
      System.err.println("column_fullnes[i]: " + column_fullnes[i]);
      pos ^= zobrist_table[column_fullnes[i]][i];
    }
    return pos;
  }

  public String playerName() {
    return "gpt4";
  }
}

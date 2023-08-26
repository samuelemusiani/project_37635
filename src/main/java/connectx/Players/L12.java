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
 * L11 + heuristic (can't play large board for now)
 */
public class L12 implements CXPlayer {
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

  public L12() {
  }

  public void initPlayer(int Rows, int Columns, int ToAlign, boolean first, int timeout_in_secs) {
    myWin = first ? 1 : -1;
    yourWin = first ? -1 : 1;
    TIMEOUT = timeout_in_secs;

    rows = Rows;
    columns = Columns;
    current_position = 0;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    isBoardTooBig = (Rows + 1) * Columns > 64;

    if (!isBoardTooBig) {
      System.err.println("Board SMALL");
      board = new CXBitBoard(Rows, Columns, ToAlign);
    } else {
      System.err.println("Board BIG");
      Random rand = new Random(System.currentTimeMillis());

      zobrist_table = new long[2 * Rows][2 * Columns];

      for (int j = 0; j < 2 * Rows; j++) {
        for (int i = 0; i < 2 * Columns; i++) {
          zobrist_table[j][i] = rand.nextInt();
        }
      }

      for (int j = 0; j < 2 * Rows; j++) {
        System.err.println(Arrays.toString(zobrist_table[j]));
      }

      column_fullnes = new int[Columns];
      static_column_fullnes = new int[Columns];

      for (int i = 0; i < Columns; i++) {
        column_fullnes[i] = 0;
        static_column_fullnes[i] = 0;
      }
    }

    evaPositionalMatrix = new int[Rows][Columns];
    int max_dist = (int) Math.sqrt(Rows * Rows + Columns * Columns) / 2;
    int center_x = Columns / 2;
    int center_y = Rows / 2;

    // // Fill half of the board with value that are larger near the center
    // for (int i = 0; i < Rows * 4 / 5; i++) {
    // for (int j = Columns / 2; j < Columns; j++) {
    // evaPositionalMatrix[i][j] = (int) (2
    // * (max_dist - Math.sqrt(Math.pow(j - center_x, 2) + Math.pow(i - center_y,
    // 2))));
    // }
    // }

    // In this way we can balance the points if black put a men on top of white
    // Basically we don't value height but "centerness"
    for (int i = Columns / 2; i < Columns; i++) {
      for (int j = 0; j < Rows; j++) {
        evaPositionalMatrix[j][i] = Columns - i;
      }
    }

    // Copy the values to the other side of the board
    for (int i = 0; i < Rows; i++) {
      for (int j = 0; j < Columns / 2; j++) {
        evaPositionalMatrix[i][j] = evaPositionalMatrix[i][Columns - j - 1];
      }
    }

    // The center column is very important
    for (int j = 0; j < Rows; j++) {
      evaPositionalMatrix[j][Columns / 2] += Columns / 2;
    }

    for (int i = Rows - 1; i >= 0; i--) {
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
    int depth = 1;
    if (am_i_fist)
      depth = Math.max(previous_search_depth - 2, 1);
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
      // depth += 2;
      depth++;
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

    // Check if the next move can make a player win
    Integer[] cols = B.getAvailableColumns();
    for (int i : cols) {
      B.markColumn(i);
      int state = B.gameState();
      if (state != 2) { // Someone won
        int val = evaluate_win(B);
        B.unmarkColumn(); // To avoid messing the board in the caller
        return val;
      }
      B.unmarkColumn();
    }
    // No player can win in the next move

    int sum = 0;
    int tmpSum = 0;

    double POSITION_WEIGHT = 4;
    double VERTICAL_WEIGHT = 0.3;
    double HORIZONTAL_WEIGHT = 1.3;
    double DIAGONAL_WEIGHT = 5;

    // Check the position of my pieces and opponent pieces
    // The more near the center the more point one piece gets
    for (int i = 0; i < B.Rows; i++) {
      for (int j = 0; j < B.Columns; j++) {
        // switch (B.cellState(B.Rows - i - 1, j)) {
        switch (B.cellState(i, j)) {
          case 1:
            tmpSum += am_i_fist ? evaPositionalMatrix[i][j] : -evaPositionalMatrix[i][j];
            break;

          case 2:
            tmpSum += am_i_fist ? -evaPositionalMatrix[i][j] : evaPositionalMatrix[i][j];
            break;

          case 0:
            break;
        }
      }
    }
    sum += tmpSum * POSITION_WEIGHT;
    tmpSum = 0;

    // Need to check the adjacent pieces

    // Horizontal
    for (int i = 0; i < B.Rows; i++) {
      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = 0; j < B.Columns; j++) {

        int cellState = B.cellState(i, j);

        // Player 1 checks
        switch (cellState) {
          case 1:
            countMen1++;
            break;

          case 2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
            // Only count white space if there are men underneath them
            if (i != 0 && B.cellState(i - 1, j) != 0) {
              countSpaces1++;
            }
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            // Only count white space if there are men underneath them
            if (i != 0 && B.cellState(i - 1, j) != 0) {
              countSpaces2++;
            }
            break;
        }
      }

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * HORIZONTAL_WEIGHT;
    tmpSum = 0;

    // Vertical
    for (int i = 0; i < B.Columns; i++) {
      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = 0; j < B.Rows; j++) {

        int cellState = B.cellState(j, i);

        // Player 1 checks
        switch (cellState) {
          case 1:
            countMen1++;
            break;

          case 2:
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
            countSpaces1++;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            countSpaces2++;
            break;
        }
      }
      if (countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces1 + countMen1) *
            (am_i_fist ? 1 : -1);
      }

      if (countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces2 + countMen2) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * VERTICAL_WEIGHT;
    tmpSum = 0;

    // Diagonal check

    // https://stackoverflow.com/a/33365042

    // Diagonal \
    for (int slice = 0; slice < B.Rows + B.Columns - 1; ++slice) {
      int z2 = slice < B.Rows ? 0 : slice - B.Rows + 1;
      int z1 = slice < B.Columns ? 0 : slice - B.Columns + 1;

      if (slice - z2 - z1 + 1 < B.ToAlign)
        continue;
      // printf("Slice %d (l: /* % */d): ", slice, slice - z2 - z1 + 1);

      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;
      for (int j = slice - z2; j >= z1; --j) {
        int cellState = B.cellState(j, slice - j);

        // Player 1 checks
        switch (cellState) {
          case 1:
            countMen1++;
            break;

          case 2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces1++;
            // }
            countSpaces1++;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces2++;
            // }
            countSpaces2++;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * DIAGONAL_WEIGHT;
    tmpSum = 0;

    // Diagonal /
    for (int slice = 0; slice < B.Columns + B.Rows - 1; ++slice) {
      int z1 = slice < B.Columns ? 0 : slice - B.Columns + 1;
      int z2 = slice < B.Rows ? 0 : slice - B.Rows + 1;

      if (slice - z2 - z1 + 1 < B.ToAlign)
        continue;

      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = (B.Rows - 1) - slice + z2; j <= (B.Rows - 1) - z1; j++) {

        int cellState = B.cellState(j, j + (slice - B.Rows + 1));

        // Player 1 checks
        switch (cellState) {
          case 1:
            countMen1++;
            break;

          case 2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces1++;
            // }
            countSpaces1++;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces2++;
            // }
            countSpaces2++;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
            (!am_i_fist ? 1 : -1);
      }
    }

    sum += tmpSum * DIAGONAL_WEIGHT;
    tmpSum = 0;

    return sum;
  }

  private int evaluate_win(CXBitBoard B) {
    evaluateCalls++;
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + 1_000_000_000; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 1_000_000_000;
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
    return "L12";
  }
}

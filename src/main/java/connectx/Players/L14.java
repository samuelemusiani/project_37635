package connectx.Players;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.Players.CXBitBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/*
 * L13 without incrementalEval
 */

public class L14 implements CXPlayer {
  private boolean isBoardTooBig;

  private L14Small pSmall;
  private L14Big pBig;

  public L14() {
  }

  public void initPlayer(int Rows, int Columns, int ToAlign, boolean first, int timeout_in_secs) {

    isBoardTooBig = (Rows + 1) * Columns > 64;
    if (isBoardTooBig) {
      pBig = new L14Big();
      pBig.initPlayer(Rows, Columns, ToAlign, first, timeout_in_secs);
    } else {
      pSmall = new L14Small();
      pSmall.initPlayer(Rows, Columns, ToAlign, first, timeout_in_secs);
    }
  }

  public int selectColumn(CXBoard B) {
    if (isBoardTooBig) {
      return pBig.selectColumn(B);
    } else {
      return pSmall.selectColumn(B);
    }
  }

  public String playerName() {
    return "L14";
  }
}

class L14Small {
  final int MAXSCORE = 1_000_000_000;

  private int myWin;
  private int yourWin;
  private int TIMEOUT;
  private long START;

  private CXBitBoard board;
  private int Rows;
  private int Columns;

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

  // Tmp
  private int fullSearches;
  private int fastSearches;
  private int tableHits;
  private int tableMiss;

  private int evaluateCalls;

  public L14Small() {
  }

  public void initPlayer(int Rows, int Columns, int ToAlign, boolean first, int timeout_in_secs) {
    myWin = first ? 1 : -1;
    yourWin = first ? -1 : 1;
    TIMEOUT = timeout_in_secs;

    this.Rows = Rows;
    this.Columns = Columns;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    board = new CXBitBoard(Rows, Columns, ToAlign);

    evaPositionalMatrix = new int[Rows][Columns];
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
      if (Columns % 2 == 0)
        evaPositionalMatrix[j][Columns / 2 - 1] += Columns / 2;
    }

    // for (int i = Rows - 1; i >= 0; i--) {
    // System.err.println(Arrays.toString(evaPositionalMatrix[i]));
    // }

  }

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis();

    if (B.numOfMarkedCells() > 0) {
      CXCell c = B.getLastMove();
      board.markColumn(c.j);
    }

    Integer[] L = reorderMoves(board);
    current_best_move = L[0];

    if (table.size() > 2_000_000) // Avoid heap errors
      table.clear();

    try {
      int move = iterativeDeepening(board.copy());
      // System.err.println("Move: " + move);
      // System.err.println("EvaluateCalls: " + evaluateCalls);
      board.markColumn(move);

      System.err.println("EvaluateCalls: " + evaluateCalls);

      return move;
    } catch (TimeoutException e) {
      board.markColumn(current_best_move);
      return current_best_move;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
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
      System.err.println("Depth: " + depth);
      fullSearches = 0;
      fastSearches = 0;
      search_not_finished = false;
      current_best_move = move_pvSearch(B, depth);
      previous_search_depth = depth;
      search_not_finished = depth < Math.max(2 * B.numOfFreeCells(), 8);
      depth += 2;
    }
    previous_search_depth += 2;

    return current_best_move;
  }

  private int move_pvSearch(CXBitBoard B, int depth) throws TimeoutException {
    checktime();
    int alpha = -(B.numOfFreeCells() / 2) - MAXSCORE;
    int beta = (B.numOfFreeCells() + 1) / 2 + MAXSCORE;
    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);
    int move = possible_moves[0];

    for (int i : possible_moves) {
      B.markColumn(i);

      long converted_position = B.hash();

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, true);
        } else {
          fastSearches++;
          score = -fastSearch(B, -alpha, depth - 1, true);
          if (score > alpha && score < beta) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, true);
          }
        }
      } else
        tableHits++;

      table.put(converted_position, score);
      table_depth.put(converted_position, depth);
      B.unmarkColumn();

      System.err.println("Score: " + score);

      if (score > alpha) {
        alpha = score;
        move = i;
      }
      bSearchPv = false;
    }

    return move;
  }

  private int pvSearch(CXBitBoard B, int alpha, int beta, int depth,
      boolean whoIsPlaying) throws TimeoutException {
    checktime();
    if (B.gameState() != 2)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      evaluateCalls++;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);

    for (int i : possible_moves) {
      B.markColumn(i);

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
          score = -fastSearch(B, -alpha, depth - 1, !whoIsPlaying);
          if (score > alpha && score < beta) { // fail-soft need to research
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
  private int fastSearch(CXBitBoard B, int beta, int depth,
      boolean whoIsPlaying) throws TimeoutException {
    checktime();
    if (B.gameState() != 2)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      evaluateCalls++;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    Integer[] possible_moves = B.getAvailableColumns();
    for (int i : possible_moves) {
      B.markColumn(i);

      Integer score = table.get(B.hash());
      Integer score_depth = table_depth.get(B.hash());
      if (score == null || score_depth < depth) {
        score = -fastSearch(B, 1 - beta, depth - 1, !whoIsPlaying);
      }

      B.unmarkColumn();

      if (score >= beta)
        return beta;
    }
    return beta - 1;
  }

  private int evaluate(CXBitBoard B) {
    double VERTICAL_WEIGHT = 0.5;
    double HORIZONTAL_WEIGHT = 1;
    double DIAGONAL_WEIGHT = 1;

    int MANSPACERATIO = 3;

    int sum = 0;
    int tmpSum = 0;

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
              tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
            // Only count white space if there are men underneath them
            // if (lastMoveRow != 0 && B.cellState(lastMoveRow - 1, j) != 0) {
            // countSpaces1++;
            // }

            if (j % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
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
            // if (lastMoveRow != 0 && B.cellState(lastMoveRow - 1, j) != 0) {
            // countSpaces2++;
            // }

            if (j % 2 == 0)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
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
        tmpSum += Math.pow(2, (countSpaces1 + countMen1)) *
            (am_i_fist ? 1 : -1);
      }

      if (countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces2 + countMen2)) *
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

      // Avoid checking small diagonals
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
              tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
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
            if ((slice - j) % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
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
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
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
              tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
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

            if ((slice - j) % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case 1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
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
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += Math.pow(2, (countSpaces2 + countMen2 * MANSPACERATIO)) *
            (!am_i_fist ? 1 : -1);
      }
    }

    sum += tmpSum * DIAGONAL_WEIGHT;
    tmpSum = 0;

    sum = (int) Math.sqrt(Math.abs(sum)) * (sum < 0 ? -1 : 1);
    return sum;
  }

  private int evaluate_win(CXBitBoard B) {
    evaluateCalls++;
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + MAXSCORE;
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - MAXSCORE;
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
}

class L14Big {
  final int MAXSCORE = 1_000_000_000;

  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;

  private int Rows;
  private int Columns;

  // Transposition table
  private HashMap<Long, Integer> table;
  private HashMap<Long, Integer> table_depth;

  private long[][] zobrist_table;
  private int[] static_column_fullnes;
  private int[] column_fullnes;
  long current_position;

  // Heuristic
  private int evaPositionalMatrix[][];
  private boolean am_i_fist;
  float POWBASE;

  // Incremental
  private int evalScore;
  private int[] rowScore;
  private int[] columnsScore;
  private int[] diagonalAScore;
  private int[] diagonalBScore;

  private int oldEvalScore;
  private int[] oldRowScore;
  private int[] oldColumnsScore;
  private int[] oldDiagonalAScore;
  private int[] oldDiagonalBScore;

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

  public L14Big() {
  }

  public void initPlayer(int Rows, int Columns, int ToAlign, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;

    this.Rows = Rows;
    this.Columns = Columns;
    current_position = 0;

    table = new HashMap<Long, Integer>();
    table_depth = new HashMap<Long, Integer>();

    am_i_fist = first;
    previous_search_depth = 1;

    switch (Rows) {
      case 20:
        this.POWBASE = 1.5f;

      case 30:
        this.POWBASE = 1.4f;

      case 40:
        this.POWBASE = 1.3f;

      case 50:
        this.POWBASE = 1.2f;

      default: // This should not appen
        this.POWBASE = 1.2f;
    }

    // Zobrist
    Random rand = new Random(System.currentTimeMillis());

    zobrist_table = new long[2 * Rows][2 * Columns];

    for (int j = 0; j < 2 * Rows; j++) {
      for (int i = 0; i < 2 * Columns; i++) {
        zobrist_table[j][i] = rand.nextInt();
      }
    }

    column_fullnes = new int[Columns];
    static_column_fullnes = new int[Columns];

    for (int i = 0; i < Columns; i++) {
      column_fullnes[i] = 0;
      static_column_fullnes[i] = 0;
    }

    evaPositionalMatrix = new int[Rows][Columns];

    // In this way we can balance the points if black put a men on top of white
    // Basically we don't value height but "centerness"
    for (int i = Columns / 2; i < Columns; i++) {
      for (int j = 0; j < Rows * 3 / 4; j++) {
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
    for (int j = 0; j < Rows * 3 / 4; j++) {
      evaPositionalMatrix[j][Columns / 2] += Columns / 2;
      if (Columns % 2 == 0)
        evaPositionalMatrix[j][Columns / 2 - 1] += Columns / 2;
    }

    // for (int i = Rows - 1; i >= 0; i--) {
    // System.err.println(Arrays.toString(evaPositionalMatrix[i]));
    // }

    // Incremental evaluation
    evalScore = 0;
    rowScore = new int[Rows];
    columnsScore = new int[Columns];
    diagonalAScore = new int[Rows + Columns]; // Upper bound
    diagonalBScore = new int[Rows + Columns]; // Upper bound

    oldEvalScore = 0;
    oldRowScore = new int[Rows];
    oldColumnsScore = new int[Columns];
    oldDiagonalAScore = new int[Rows + Columns]; // Upper bound
    oldDiagonalBScore = new int[Rows + Columns]; // Upper bound

    for (int i = 0; i < Rows; i++) {
      rowScore[i] = 0;
      oldRowScore[i] = 0;
    }

    for (int i = 0; i < Columns; i++) {
      columnsScore[i] = 0;
      oldColumnsScore[i] = 0;
    }

    for (int i = 0; i < Columns + Rows; i++) {
      diagonalAScore[i] = 0;
      diagonalBScore[i] = 0;
      oldDiagonalAScore[i] = 0;
      oldDiagonalBScore[i] = 0;
    }
  }

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis();

    if (B.numOfMarkedCells() > 0) {
      CXCell c = B.getLastMove();
      // I don't need to check if i can win
      oldEvalScore = decrementalEvaluate(B, c.i, c.j, this.oldEvalScore,
          this.oldRowScore, this.oldColumnsScore, this.oldDiagonalAScore,
          this.oldDiagonalBScore);
    }

    evalScore = oldEvalScore;

    for (int i = 0; i < Rows; i++)
      rowScore[i] = oldRowScore[i];

    for (int i = 0; i < Columns; i++)
      columnsScore[i] = oldColumnsScore[i];

    for (int i = 0; i < Columns + Rows; i++) {
      diagonalAScore[i] = oldDiagonalAScore[i];
      diagonalBScore[i] = oldDiagonalBScore[i];
    }

    Integer[] L = reorderMoves(B);
    current_best_move = L[0];

    if (table.size() > 2_000_000) // Avoid heap errors
      table.clear();

    try {
      for (int i = 0; i < Columns; i++) {
        column_fullnes[i] = static_column_fullnes[i];
      }

      if (B.numOfMarkedCells() > 0) {
        int c = B.getLastMove().j;
        current_position = zobristMakeMove(current_position, c, true);
        static_column_fullnes[c]++;
      }

      int move = iterativeDeepening(B.copy(), current_position);

      column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
      current_position = zobristMakeMove(current_position, move, false);
      static_column_fullnes[move]++;

      CXCell c = B.getLastMove();
      oldEvalScore = decrementalEvaluate(B, c.i, c.j, this.oldEvalScore,
          this.oldRowScore, this.oldColumnsScore, this.oldDiagonalAScore,
          this.oldDiagonalBScore);

      return move;
    } catch (TimeoutException e) {
      column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
      current_position = zobristMakeMove(current_position, current_best_move,
          false);
      static_column_fullnes[current_best_move]++;

      if (B.numOfMarkedCells() > 0) {
        CXCell c = B.getLastMove();
        oldEvalScore = decrementalEvaluate(B, c.i, c.j, this.oldEvalScore,
            this.oldRowScore, this.oldColumnsScore, this.oldDiagonalAScore,
            this.oldDiagonalBScore);
      }

      return current_best_move;
    }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (98.0 / 100.0))
      throw new TimeoutException();
  }

  private int iterativeDeepening(CXBoard B, long position) throws TimeoutException {
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
      search_not_finished = false;
      current_best_move = move_pvSearch(B, depth, position);
      previous_search_depth = depth;
      search_not_finished = depth < Math.max(2 * B.numOfFreeCells(), 8);
      depth += 2;
    }
    previous_search_depth += 2;

    return current_best_move;
  }

  private int move_pvSearch(CXBoard B, int depth, long position) throws TimeoutException {
    checktime();
    int alpha = -(B.numOfFreeCells() / 2) - MAXSCORE;
    int beta = (B.numOfFreeCells() + 1) / 2 + MAXSCORE;
    boolean bSearchPv = true;
    Integer[] possible_moves = reorderMoves(B);
    int move = possible_moves[0];

    for (int i : possible_moves) {
      B.markColumn(i);
      position = zobristMakeMove(position, i, false);

      // Incremental Evaluation
      CXCell c = B.getLastMove();
      int preEvalScore = evalScore;
      int preRowScore = rowScore[c.i];
      int preColumnScore = columnsScore[c.j];
      int preDiagonalAScore = diagonalAScore[c.i + c.j];
      int preDiagonalBScore = diagonalBScore[c.i + c.j];
      incrementalEvaluate(B);

      Integer score = table.get(position);
      Integer score_depth = table_depth.get(position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
        } else {
          fastSearches++;
          score = -fastSearch(B, -alpha, depth - 1, true, position);
          if (score > alpha && score < beta) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
          }
        }
      } else
        tableHits++;

      table.put(position, score);
      table_depth.put(position, depth);
      B.unmarkColumn();
      position = zobristUnmakeMove(position, i, false);

      // Reset incremental eval
      evalScore = preEvalScore;
      rowScore[c.i] = preRowScore;
      columnsScore[c.j] = preColumnScore;
      diagonalAScore[c.i + c.j] = preDiagonalAScore;
      diagonalBScore[c.i + c.j] = preDiagonalBScore;

      if (score > alpha) {
        alpha = score;
        move = i;
      }
      bSearchPv = false;
    }

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

      // Incremental Evaluation
      CXCell c = B.getLastMove();
      int preEvalScore = evalScore;
      int preRowScore = rowScore[c.i];
      int preColumnScore = columnsScore[c.j];
      int preDiagonalAScore = diagonalAScore[c.i + c.j];
      int preDiagonalBScore = diagonalBScore[c.i + c.j];
      incrementalEvaluate(B);

      Integer score = table.get(position);
      Integer score_depth = table_depth.get(position);
      if (score == null || score_depth < depth) {
        tableMiss++;
        if (bSearchPv) {
          fullSearches++;
          score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
        } else {
          fastSearches++;
          score = -fastSearch(B, -alpha, depth - 1, !whoIsPlaying, position);
          if (score > alpha && score < beta) { // fail-soft need to research
            fullSearches++;
            score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
          }
        }
      } else
        tableHits++;

      table.put(position, score);
      table_depth.put(position, depth);

      B.unmarkColumn();
      position = zobristUnmakeMove(position, i, whoIsPlaying);

      // Reset incremental eval
      evalScore = preEvalScore;
      rowScore[c.i] = preRowScore;
      columnsScore[c.j] = preColumnScore;
      diagonalAScore[c.i + c.j] = preDiagonalAScore;
      diagonalBScore[c.i + c.j] = preDiagonalBScore;

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
  private int fastSearch(CXBoard B, int beta, int depth,
      boolean whoIsPlaying, long position) throws TimeoutException {
    checktime();
    if (B.gameState() != CXGameState.OPEN)
      return evaluate_win(B) * (whoIsPlaying ? -1 : 1);

    if (depth <= 0) {
      search_not_finished = true;
      return evaluate(B) * (whoIsPlaying ? -1 : 1);
    }

    Integer[] possible_moves = B.getAvailableColumns();
    for (int i : possible_moves) {
      B.markColumn(i);
      // Incremental Evaluation
      CXCell c = B.getLastMove();
      int preEvalScore = evalScore;
      int preRowScore = rowScore[c.i];
      int preColumnScore = columnsScore[c.j];
      int preDiagonalAScore = diagonalAScore[c.i + c.j];
      int preDiagonalBScore = diagonalBScore[c.i + c.j];
      incrementalEvaluate(B);

      int score = -fastSearch(B, 1 - beta, depth - 1, !whoIsPlaying, position);

      B.unmarkColumn();
      // Reset incremental eval
      evalScore = preEvalScore;
      rowScore[c.i] = preRowScore;
      columnsScore[c.j] = preColumnScore;
      diagonalAScore[c.i + c.j] = preDiagonalAScore;
      diagonalBScore[c.i + c.j] = preDiagonalBScore;

      if (score >= beta)
        return beta;
    }
    return beta - 1;
  }

  private int incrementalEvaluate(CXBoard B) {
    CXCell lastMove = B.getLastMove();
    int lastMoveRow = lastMove.i;
    int lastMoveColumn = lastMove.j;

    this.evalScore = decrementalEvaluate(B, lastMoveRow, lastMoveColumn,
        this.evalScore, this.rowScore, this.columnsScore, this.diagonalAScore,
        this.diagonalBScore);
    return this.evalScore;
  }

  private int decrementalEvaluate(CXBoard B, int lastMoveRow,
      int lastMoveColumn, int evalScore, int[] rowScore, int[] columnsScore,
      int[] diagonalAScore, int[] diagonalBScore) {

    int sum = evalScore * evalScore * (evalScore < 0 ? -1 : 1);
    sum -= rowScore[lastMoveRow];
    sum -= columnsScore[lastMoveColumn];
    sum -= diagonalAScore[lastMoveColumn + lastMoveRow];
    sum -= diagonalBScore[lastMoveColumn + lastMoveRow];

    // System.err.println("rowScore: " + rowScore[lastMoveRow]);
    // System.err.println("columnsScore: " + columnsScore[lastMoveColumn]);

    double VERTICAL_WEIGHT = 0.5;
    double HORIZONTAL_WEIGHT = 1;
    double DIAGONAL_WEIGHT = 1;

    int MANSPACERATIO = 3;

    int tmpSum = 0;

    // Horizontal
    {
      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = 0; j < B.N; j++) {

        CXCellState cellState = B.cellState(lastMoveRow, j);

        // Player 1 checks
        switch (cellState) {
          case P1:
            countMen1++;
            break;

          case P2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
            // Only count white space if there are men underneath them
            // if (lastMoveRow != 0 && B.cellState(lastMoveRow - 1, j) != 0) {
            // countSpaces1++;
            // }

            if (j % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case P1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            // Only count white space if there are men underneath them
            // if (lastMoveRow != 0 && B.cellState(lastMoveRow - 1, j) != 0) {
            // countSpaces2++;
            // }

            if (j % 2 == 0)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * HORIZONTAL_WEIGHT;
    rowScore[lastMoveRow] = (int) (tmpSum * HORIZONTAL_WEIGHT);
    tmpSum = 0;

    // Vertical
    {
      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = 0; j < B.M; j++) {

        CXCellState cellState = B.cellState(j, lastMoveColumn);

        // Player 1 checks
        switch (cellState) {
          case P1:
            countMen1++;
            break;

          case P2:
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
            countSpaces1++;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case P1:
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            countSpaces2++;
            break;
        }
      }
      if (countMen1 + countSpaces1 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1)) *
            (am_i_fist ? 1 : -1);
      }

      if (countMen2 + countSpaces2 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2)) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * VERTICAL_WEIGHT;
    columnsScore[lastMoveColumn] = (int) (tmpSum * VERTICAL_WEIGHT);
    tmpSum = 0;

    // Diagonal check

    // Diagonal \
    diagonal1: {
      int slice = lastMoveRow + lastMoveColumn;

      int z2 = slice < B.M ? 0 : slice - B.M + 1;
      int z1 = slice < B.N ? 0 : slice - B.N + 1;

      // Avoid checking small diagonals
      if (slice - z2 - z1 + 1 < B.X)
        break diagonal1;

      // printf("Slice %d (l: /* % */d): ", slice, slice - z2 - z1 + 1);

      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;
      for (int j = slice - z2; j >= z1; --j) {
        CXCellState cellState = B.cellState(j, slice - j);

        // Player 1 checks
        switch (cellState) {
          case P1:
            countMen1++;
            break;

          case P2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces1++;
            // }
            if ((slice - j) % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case P1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces2++;
            // }
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
            (!am_i_fist ? 1 : -1);
      }
    }
    sum += tmpSum * DIAGONAL_WEIGHT;
    diagonalAScore[lastMoveRow + lastMoveColumn] = (int) (tmpSum * VERTICAL_WEIGHT);
    tmpSum = 0;

    // Diagonal /
    diagonal2: {
      int slice = lastMoveColumn + lastMoveRow;
      int z1 = slice < B.N ? 0 : slice - B.N + 1;
      int z2 = slice < B.M ? 0 : slice - B.M + 1;

      if (slice - z2 - z1 + 1 < B.X)
        break diagonal2;

      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = (B.M - 1) - slice + z2; j <= (B.M - 1) - z1; j++) {

        CXCellState cellState = B.cellState(j, j + (slice - B.M + 1));

        // Player 1 checks
        switch (cellState) {
          case P1:
            countMen1++;
            break;

          case P2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
                  (am_i_fist ? 1 : -1);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces1++;
            // }

            if ((slice - j) % 2 == 0)
              countSpaces1++;
            else
              countSpaces1 += 2;
            break;
        }

        // Player 2 checks
        switch (cellState) {
          case P1:
            if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
              tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
                  (!am_i_fist ? 1 : -1);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            // // Only count white space if there are men underneath them
            // if (i != 0 && B.cellState(i - 1, j) != 0) {
            // countSpaces2++;
            // }
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces1 + countMen1 * MANSPACERATIO)) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += Math.pow(POWBASE, (countSpaces2 + countMen2 * MANSPACERATIO)) *
            (!am_i_fist ? 1 : -1);
      }
    }

    sum += tmpSum * DIAGONAL_WEIGHT;
    diagonalBScore[lastMoveRow + lastMoveColumn] = (int) (tmpSum * VERTICAL_WEIGHT);
    tmpSum = 0;

    sum = (int) Math.sqrt(Math.abs(sum)) * (sum < 0 ? -1 : 1);

    evalScore = sum;
    return evalScore;
  }

  private int evaluate(CXBoard B) {

    // Check if the next move can make a player win
    if (B.numOfMarkedCells() > B.X * 2) {
      Integer[] cols = B.getAvailableColumns();
      for (int i : cols) {
        B.markColumn(i);
        if (B.gameState() != CXGameState.OPEN) { // Someone won
          int val = evaluate_win(B);
          B.unmarkColumn(); // To avoid messing the board in the caller
          return val;
        }
        B.unmarkColumn();
      }
      // No player can win in the next move
    }

    return evalScore;
  }

  private int evaluate_win(CXBoard B) {
    evaluateCalls++;
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + MAXSCORE;
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - MAXSCORE;
    else
      return 0;
  }

  private Integer[] reorderMoves(CXBoard B) {
    Integer[] r = B.getAvailableColumns();

    // int delta = 0;
    //
    // for (int i = 0; i < l; i++) {
    // if (i % 2 == 1)
    // delta++;
    //
    // r[i] = m[l / 2 + (i % 2 == 0 ? 1 : -1) * delta];
    // }

    int tmp;
    tmp = r[0];
    r[0] = r[B.N / 2];
    r[B.N / 2] = tmp;

    tmp = r[1];
    r[1] = r[B.N / 2 + 1];
    r[B.N / 2 + 1] = tmp;
    return r;
  }

  private long zobristMakeMove(long pos, int i, boolean whoHasPlayed) {
    if (whoHasPlayed) {
      pos ^= zobrist_table[column_fullnes[i] + Rows][i + Columns];
    } else {
      pos ^= zobrist_table[column_fullnes[i]][i];
    }

    column_fullnes[i]++;

    return pos;
  }

  private long zobristUnmakeMove(long pos, int i, boolean whoHasPlayed) {
    column_fullnes[i]--;

    if (whoHasPlayed) {
      pos ^= zobrist_table[column_fullnes[i] + Rows][i + Columns];
    } else {
      pos ^= zobrist_table[column_fullnes[i]][i];
    }

    return pos;
  }
}

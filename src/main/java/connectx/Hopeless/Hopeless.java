package connectx.Hopeless;

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

public class Hopeless implements CXPlayer {
  private boolean isBoardTooBig;

  private L17Small pSmall;
  private L17Big pBig;

  public Hopeless() {
  }

  public void initPlayer(int Rows, int Columns, int ToAlign, boolean first, int timeout_in_secs) {

    isBoardTooBig = (Rows + 1) * Columns > 64;
    if (isBoardTooBig) {
      pBig = new L17Big();
      pBig.initPlayer(Rows, Columns, ToAlign, first, timeout_in_secs);
    } else {
      pSmall = new L17Small();
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
    return "Hopeless";
  }
}

class L17Small {
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
  private boolean am_i_fist;

  // Iterative deepening
  private int current_best_move;
  private boolean search_not_finished;
  private int previous_search_depth;

  // Move order
  private int[] movesOrdered;

  private int evaluateCalls;

  public L17Small() {
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

    // Moves reoring
    movesOrdered = new int[Columns];
    int[] m = new int[Columns];
    for (int i = 0; i < Columns; i++)
      m[i] = i;

    int delta = 0;
    for (int i = 0; i < Columns; i++) {
      if (i % 2 == 1)
        delta++;

      movesOrdered[i] = m[Columns / 2 + (i % 2 == 0 ? 1 : -1) * delta];
    }
  }

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis();

    if (B.numOfMarkedCells() > 0) {
      CXCell c = B.getLastMove();
      board.markColumn(c.j);
    }

    current_best_move = 0;

    if (table.size() > 2_000_000) // Avoid heap errors
      table.clear();

    try {
      int move = iterativeDeepening(board.copy());
      board.markColumn(move);

      System.err.println("EvaluateCalls: " + evaluateCalls);

      return move;
    } catch (TimeoutException e) {
      board.markColumn(current_best_move);
      System.err.println("EvaluateCalls: " + evaluateCalls);
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

    search_not_finished = true;
    while (search_not_finished) {
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
    int move = 0;

    for (int i : this.movesOrdered) {
      if (B.fullColumn(i))
        continue;

      B.markColumn(i);

      long converted_position = B.hash();

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        if (bSearchPv) {
          score = -pvSearch(B, -beta, -alpha, depth - 1, true);
        } else {
          score = -fastSearch(B, -alpha, depth - 1, true);
          if (score > alpha && score < beta) { // Need to research
            score = -pvSearch(B, -beta, -alpha, depth - 1, true);
          }
        }
      }

      table.put(converted_position, score);
      table_depth.put(converted_position, depth);
      B.unmarkColumn();

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

    for (int i : this.movesOrdered) {
      if (B.fullColumn(i))
        continue;

      B.markColumn(i);

      long converted_position = B.hash();

      Integer score = table.get(converted_position);
      Integer score_depth = table_depth.get(converted_position);
      if (score == null || score_depth < depth) {
        if (bSearchPv) {
          score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying);
        } else {
          score = -fastSearch(B, -alpha, depth - 1, !whoIsPlaying);
          if (score > alpha && score < beta) { // need to research
            score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying);
          }
        }
      }

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

  // zero window search, returns either beta-1 or beta
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

    for (int i : this.movesOrdered) {
      if (B.fullColumn(i))
        continue;

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
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            if (j % 2 == 0)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }
    sum += tmpSum;
    tmpSum = 0;

    // Diagonal \
    for (int slice = 0; slice < B.Rows + B.Columns - 1; ++slice) {
      int z2 = slice < B.Rows ? 0 : slice - B.Rows + 1;
      int z1 = slice < B.Columns ? 0 : slice - B.Columns + 1;

      // Avoid checking small diagonals
      if (slice - z2 - z1 + 1 < B.ToAlign)
        continue;

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
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:
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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }
    sum += tmpSum;
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
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case 0:

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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case 2:
            countMen2++;
            break;

          case 0:
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }

    sum += tmpSum;
    tmpSum = 0;

    if (!am_i_fist)
      sum = -sum;

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
}

class L17Big {
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
  private boolean am_i_fist;

  // Iterative deepening
  private int current_best_move;
  private boolean search_not_finished;
  private int previous_search_depth;

  // Move order
  private int[] movesOrdered;

  public L17Big() {
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

    // Moves reoring
    movesOrdered = new int[Columns];
    int[] m = new int[Columns];
    for (int i = 0; i < Columns; i++)
      m[i] = i;

    int delta = 0;
    for (int i = 0; i < Columns; i++) {
      if (i % 2 == 1)
        delta++;

      movesOrdered[i] = m[Columns / 2 + (i % 2 == 0 ? 1 : -1) * delta];
    }
  }

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis();

    current_best_move = 0;

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

      int move = iterativeDeepening(B, current_position);

      column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
      current_position = zobristMakeMove(current_position, move, false);
      static_column_fullnes[move]++;

      return move;
    } catch (TimeoutException e) {
      column_fullnes[current_best_move] = static_column_fullnes[current_best_move];
      current_position = zobristMakeMove(current_position, current_best_move,
          false);
      static_column_fullnes[current_best_move]++;

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

    search_not_finished = true;
    while (search_not_finished) {
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

    int move = 0;

    for (int i : this.movesOrdered) {
      if (B.fullColumn(i))
        continue;

      B.markColumn(i);
      position = zobristMakeMove(position, i, false);

      Integer score = table.get(position);
      Integer score_depth = table_depth.get(position);
      if (score == null || score_depth < depth) {
        if (bSearchPv) {
          score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
        } else {
          score = -fastSearch(B, -alpha, depth - 1, true, position);
          if (score > alpha && score < beta) { // need to research
            score = -pvSearch(B, -beta, -alpha, depth - 1, true, position);
          }
        }
      }

      table.put(position, score);
      table_depth.put(position, depth);
      B.unmarkColumn();
      position = zobristUnmakeMove(position, i, false);

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

    for (int i : this.movesOrdered) {
      if (B.fullColumn(i))
        continue;

      B.markColumn(i);
      position = zobristMakeMove(position, i, whoIsPlaying);

      Integer score = table.get(position);
      Integer score_depth = table_depth.get(position);
      if (score == null || score_depth < depth) {
        if (bSearchPv) {
          score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
        } else {
          score = -fastSearch(B, -alpha, depth - 1, !whoIsPlaying, position);
          if (score > alpha && score < beta) { // need to research
            score = -pvSearch(B, -beta, -alpha, depth - 1, !whoIsPlaying, position);
          }
        }
      }

      table.put(position, score);
      table_depth.put(position, depth);

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

  // zero window search, returns either beta-1 or beta
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

      position = zobristMakeMove(position, i, whoIsPlaying);

      Integer score = table.get(position);
      Integer score_depth = table_depth.get(position);

      if (score == null || score_depth < depth) {
        score = -fastSearch(B, 1 - beta, depth - 1, !whoIsPlaying, position);
      }
      position = zobristUnmakeMove(position, i, whoIsPlaying);

      B.unmarkColumn();

      if (score >= beta)
        return beta;
    }
    return beta - 1;
  }

  private int evaluate(CXBoard B) {
    int MANSPACERATIO = 5;

    int sum = 0;
    int tmpSum = 0;

    // Horizontal
    for (int i = 0; i < B.M; i++) {
      int countMen1 = 0;
      int countMen2 = 0;
      int countSpaces1 = 0;
      int countSpaces2 = 0;

      for (int j = 0; j < B.N; j++) {

        CXCellState cellState = B.cellState(i, j);

        // Player 1 checks
        switch (cellState) {
          case P1:
            countMen1++;
            break;

          case P2:
            if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            if (j % 2 == 0)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }
    sum += tmpSum;
    tmpSum = 0;

    // Diagonal \
    for (int slice = 0; slice < B.M + B.N - 1; ++slice) {
      int z2 = slice < B.M ? 0 : slice - B.M + 1;
      int z1 = slice < B.N ? 0 : slice - B.N + 1;

      // Avoid checking small diagonals
      if (slice - z2 - z1 + 1 < B.X)
        continue;

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
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }
    sum += tmpSum;
    tmpSum = 0;

    // Diagonal /
    for (int slice = 0; slice < B.N + B.M - 1; ++slice) {
      int z1 = slice < B.N ? 0 : slice - B.N + 1;
      int z2 = slice < B.M ? 0 : slice - B.M + 1;

      if (slice - z2 - z1 + 1 < B.X)
        continue;

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
              tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
            }
            countMen1 = 0;
            countSpaces1 = 0;
            break;

          case FREE:
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
              tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
            }
            countMen2 = 0;
            countSpaces2 = 0;
            break;

          case P2:
            countMen2++;
            break;

          case FREE:
            if ((slice - j) % 2 == 1)
              countSpaces2++;
            else
              countSpaces2 += 2;
            break;
        }
      }
      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.X) {
        tmpSum += (countSpaces1 + countMen1 * MANSPACERATIO);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.X) {
        tmpSum += -(countSpaces2 + countMen2 * MANSPACERATIO);
      }
    }

    sum += tmpSum;
    tmpSum = 0;

    if (!am_i_fist)
      sum = -sum;

    return sum;
  }

  private int evaluate_win(CXBoard B) {
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + MAXSCORE;
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - MAXSCORE;
    else
      return 0;
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

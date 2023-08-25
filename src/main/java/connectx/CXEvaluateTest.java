package connectx;

import connectx.Players.CXBitBoard;
import java.util.Arrays;
import java.util.Scanner;

public class CXEvaluateTest {
  public static void main(String[] args) {
    CXEvaluateTest b = new CXEvaluateTest();
    b.init();
    b.run();
  }

  final int Rows = 6;
  final int Columns = 7;
  final int toAlign = 4;

  int myWin;
  int yourWin;
  final boolean am_i_fist = true;

  private int evaPositionalMatrix[][];

  public void init() {
    myWin = am_i_fist ? 1 : -1;
    yourWin = am_i_fist ? -1 : 1;

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

  public void run() {
    CXBitBoard board = new CXBitBoard(Rows, Columns, toAlign);

    Scanner scan = new Scanner(System.in);

    boolean whoIsPlaying = false;

    while (board.gameState() == 2) {
      Integer[] moves = board.getAvailableColumns();

      System.err.println("Moves: " + Arrays.toString(moves));

      for (int i : moves) {
        board.markColumn(i);
        System.err.println("Move, Eval: " + i + " " + evaluate(board) * (whoIsPlaying ? -1 : 1));
        board.unmarkColumn();
      }

      int col = scan.nextInt();

      if (col >= 0)
        board.markColumn(col);
      else
        board.unmarkColumn();

      whoIsPlaying = !whoIsPlaying;
    }

    System.err.println("gameState: " + board.gameState());

    scan.close();
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

    double POSITION_WEIGHT = B.numOfFreeCells() / 4;
    double VERTICAL_WEIGHT = 0.3;
    double HORIZONTAL_WEIGHT = 1.3;
    double DIAGONAL_WEIGHT = 1.5;

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
    // System.err.println("tmpSum: " + tmpSum);
    // System.err.println("POSITION_WEIGHT: " + POSITION_WEIGHT);
    System.err.print("Pos: " + tmpSum * POSITION_WEIGHT);
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
            countSpaces2++;
            break;
        }
      }
      // System.err.print("tmpSum1: " + tmpSum + " men1: " + countMen1 +
      // " men2: " + countMen2 + " blank1: " + countSpaces1 + " blank2: " +
      // countSpaces2);

      // if we didn't hit the opponent men we need to evaluate
      if (countMen1 >= 2 && countMen1 + countSpaces1 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces1 + countMen1 * 2) *
            (am_i_fist ? 1 : -1);
      }
      if (countMen2 >= 2 && countMen2 + countSpaces2 >= B.ToAlign) {
        tmpSum += 2 * (countSpaces2 + countMen2 * 2) *
            (!am_i_fist ? 1 : -1);
      }
      // System.err.println(" tmpSum2: " + tmpSum);
    }
    sum += tmpSum * HORIZONTAL_WEIGHT;
    System.err.print(" Horizzontal: " + tmpSum * HORIZONTAL_WEIGHT);
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
            if (countMen1 + countSpaces1 >= B.ToAlign) {
              tmpSum += (Math.pow(2, countSpaces1 + countMen1 * 2) - 1) *
                  (am_i_fist ? 1 : -1);
            }
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
            if (countMen2 + countSpaces2 >= B.ToAlign) {
              tmpSum += (Math.pow(2, countSpaces2 + countMen2 * 2) - 1) *
                  (!am_i_fist ? 1 : -1);
            }
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
    }
    sum += tmpSum * VERTICAL_WEIGHT;
    System.err.print(" Vetical: " + tmpSum * VERTICAL_WEIGHT);
    System.err.println();
    tmpSum = 0;

    // Diagonal check??

    return sum;
  }

  private int evaluate_win(CXBitBoard B) {
    if (B.gameState() == myWin)
      return (B.numOfFreeCells() + 1) / 2 + 1_000_000_000; // To avoid 0 meaning win and draw
    else if (B.gameState() == yourWin)
      return -(B.numOfFreeCells() / 2) - 1_000_000_000;
    else
      return 0;
  }
}

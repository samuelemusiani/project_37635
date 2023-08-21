package connectx;

import connectx.Players.CXBitBoard;
import java.util.Arrays;
import java.util.Scanner;

public class CXBitTest {
  public static void main(String[] args) {
    CXBitBoard board = new CXBitBoard(4, 6, 4);

    Scanner scan = new Scanner(System.in);

    while (board.gameState() == 2) {
      Integer[] moves = board.getAvailableColumns();

      System.err.println("Moves: " + Arrays.toString(moves));

      int col = scan.nextInt();

      if (col >= 0)
        board.markColumn(col);
      else
        board.unmarkColumn();

      for (int i = 0; i < board.Columns; i++) {
        for (int j = 0; j < board.Rows; j++) {
          System.err.println("State ( " + i + ", " + j + "): " + board.cellState(i, j));
        }
      }
    }

    System.err.println("gameState: " + board.gameState());

    scan.close();
  }
}

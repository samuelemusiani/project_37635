package connectx;

import connectx.Players.CXBitBoard;
import java.util.Arrays;
import java.util.Scanner;

public class CXBitTest {
  public static void main(String[] args) {
    CXBitBoard board = new CXBitBoard(7, 7, 5);

    Scanner scan = new Scanner(System.in);

    while (board.gameState() == 2) {
      Integer[] moves = board.getAvailableColumns();

      System.err.println("Moves: " + Arrays.toString(moves));

      int col = scan.nextInt();

      if (col >= 0)
        board.markColumn(col);
      else
        board.unmarkColumn();

    }

    System.err.println("gameState: " + board.gameState());

    scan.close();
  }
}

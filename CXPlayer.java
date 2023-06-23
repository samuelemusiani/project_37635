/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx;

public interface CXPlayer {
	/**
	 * Interface for a (M,N)-game software player.
	 * <p>
	 * The implementing classes need to provide a constructor that takes no
	 * arguments. The CxPlayer is initialized through the <code>initPlayer</code>
	 * method.
	 * </p>
	 */

	/**
	 * Initialize the (M,N) Player
	 *
	 * @param M               Board rows
	 * @param N               Board columns
	 * @param X               Number of coins to be aligned (horizontally, vertically, diagonally) for a win
	 * @param first           True if it is the first player, False otherwise
	 * @param timeout_in_secs Maximum amount of time (in seconds) for initialization and for selecting a column
	 */
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs);

	/**
	 * Select a move (a column index)
	 *
	 * @param B A CXBoard object representing the current state of the game 
	 *
	 * @return a column index
	 */
	public int selectColumn(CXBoard B);

	/**
	 * Returns the player name
	 *
	 * @return string
	 */
	public String playerName();

}

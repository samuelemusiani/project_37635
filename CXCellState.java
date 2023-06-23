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

/**
 * <code>CXBoard</code> cell states
 *
 * @see CXCell CXCell
 * @see CXBoard CxBoard
 */

public enum CXCellState {

	/**
	 * Cell selected by Player 1
	 */
	P1,
	/**
	 * Cell selected by Player 2
	 */
	P2,
	/**
	 * Free cell
	 */
	FREE
}

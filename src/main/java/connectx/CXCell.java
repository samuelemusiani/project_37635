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
 * Describes the state of a cell in the <code>CXBoard</code>.
 * 
 * @see CXBoard CXBoard
 */
public class CXCell {

	/**
	 * Cell row index
	 */
	public final int i;
	/**
	 * Cell column index
	 */
	public final int j;
	/**
	 * Cell state
	 */
	public final CXCellState state;

	/**
	 * Allocates a cell
	 * 
	 * @param i cell row index
	 * 
	 * @param j cell column index
	 * 
	 * @param state cell state
	 */
	public CXCell(int i, int j, CXCellState state) {
		this.i = i;
		this.j = j;
		this.state = state;
	}
}

/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2023 by original authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.buliasz.opensudoku2.game

private const val numRows = 9
private const val numCols = 9
private const val numValues = 9
private const val numConstraints = 4
private const val numCells = numRows * numCols

class SudokuSolver {
	private lateinit var mLinkedList: Array<Array<Node?>>
	private lateinit var mHead: Node
	private var mSolution: ArrayList<Node> = ArrayList()

	init {
		initializeNodesMatrix()
		initializeNodesLinks()
	}
	/* ---------------PUBLIC FUNCTIONS--------------- */
	/**
	 * Modifies linked list based on the original state of the board
	 */
	fun setPuzzle(mCells: CellCollection) {
		val board = mCells.cells
		for (row in 0..8) {
			for (col in 0..8) {
				val cell = board[row][col]
				if (!cell.isEditable) {
					val matrixRow = cellToRow(row, col, cell.value - 1)
					val matrixCol = 9 * row + col // calculates column of node based on cell constraint
					val rowNode = mLinkedList[matrixRow][matrixCol]!!
					var rightNode = rowNode
					do {
						cover(rightNode)
						rightNode = rightNode.right
					} while (rightNode !== rowNode)
				}
			}
		}
	}

	fun solve(): ArrayList<IntArray> {
		mSolution = dlx()
		val finalValues = ArrayList<IntArray>()
		for (node in mSolution) {
			val matrixRow = node.rowID
			val rowColVal = rowToCell(matrixRow)
			finalValues.add(rowColVal)
		}
		return finalValues
	}

	/* ---------------FUNCTIONS TO IMPLEMENT SOLVER--------------- */
	private fun initializeNodesMatrix() {
		mLinkedList = Array(numRows * numCols * numValues + 1) { arrayOfNulls(numCells * numConstraints) }

		// add row of 1's for column headers
		mLinkedList[0] = Array(numCells * numConstraints) { Node() }

		// calculate column where constraint will go
		val rowShift = numCells
		val colShift = numCells * 2
		val blockShift = numCells * 3
		var cellColumn = 0
		var rowColumn = 0
		var blockColumn = 0
		for (row in 0..<numRows) {
			var colColumn = 0
			for (col in 0..<numCols) {
				for (value in 0..<numValues) {
					val matrixRow = cellToRow(row, col, value)

					// cell
					mLinkedList[matrixRow][cellColumn] = Node()

					// row
					mLinkedList[matrixRow][rowColumn + rowShift] = Node()
					rowColumn++

					// col
					mLinkedList[matrixRow][colColumn + colShift] = Node()
					colColumn++

					// block
					mLinkedList[matrixRow][blockColumn + blockShift] = Node()
					blockColumn++
				}
				cellColumn++
				rowColumn -= numCols
				if (col % 3 != 2) {
					blockColumn -= numCols
				}
			}
			rowColumn += numCols
			if (row % 3 != 2) {
				blockColumn -= numCols * 3
			}
		}
	}

	private fun initializeNodesLinks() {
		mHead = Node()
		val rows = mLinkedList.size
		val cols = mLinkedList[0].size

		// link nodes in mLinkedList
		for (i in 0..<rows) {
			for (j in 0..<cols) {
				if (mLinkedList[i][j] == null) continue

				var a: Int
				var b: Int

				// link left
				a = i
				b = j
				do {
					b = moveLeft(b, cols)
				} while (mLinkedList[a][b] == null)
				mLinkedList[i][j]!!.left = mLinkedList[a][b]!!

				// link right
				a = i
				b = j
				do {
					b = moveRight(b, cols)
				} while (mLinkedList[a][b] == null)
				mLinkedList[i][j]!!.right = mLinkedList[a][b]!!

				// link up
				a = i
				b = j
				do {
					a = moveUp(a, rows)
				} while (mLinkedList[a][b] == null)
				mLinkedList[i][j]!!.up = mLinkedList[a][b]!!

				// link up
				a = i
				b = j
				do {
					a = moveDown(a, rows)
				} while (mLinkedList[a][b] == null)
				mLinkedList[i][j]!!.down = mLinkedList[a][b]!!

				// initialize remaining node info
				mLinkedList[i][j]!!.columnHeader = mLinkedList[0][j]!!
				mLinkedList[i][j]!!.rowID = i
				mLinkedList[i][j]!!.colID = j
			}
		}

		// link head node
		mHead.right = mLinkedList[0][0]!!
		mHead.left = mLinkedList[0][cols - 1]!!
		mLinkedList[0][0]!!.left = mHead
		mLinkedList[0][cols - 1]!!.right = mHead
	}

	/**
	 * Dancing links algorithm
	 *
	 * @return array of solution nodes or empty array if no solution exists
	 */
	private fun dlx(): ArrayList<Node> {
		if (mHead.right === mHead) {
			return mSolution    // all nodes covered
		}

		var colNode = chooseLeastCountNode()
		cover(colNode)

		var rowNode = colNode.down
		while (rowNode !== colNode) {
			mSolution.add(rowNode)
			var rightNode = rowNode.right
			while (rightNode !== rowNode) {
				cover(rightNode)
				rightNode = rightNode.right
			}
			val tempSolution = dlx()
			if (tempSolution.isNotEmpty()) {
				return tempSolution
			}

			// undo operations and try the next row
			mSolution.removeAt(mSolution.size - 1)
			colNode = rowNode.columnHeader
			var leftNode = rowNode.left
			while (leftNode !== rowNode) {
				uncover(leftNode)
				leftNode = leftNode.left
			}
			rowNode = rowNode.down
		}
		uncover(colNode)
		return ArrayList()
	}
	/* ---------------UTILITY FUNCTIONS--------------- */
	/**
	 * Converts from puzzle cell to constraint matrix
	 *
	 * @param row             0-8 index
	 * @param col             0-8 index
	 * @param value           0-8 index (representing values 1-9)
	 * @return row in mConstraintMatrix corresponding to cell indices and value
	 */
	private fun cellToRow(row: Int, col: Int, value: Int): Int {
		var matrixRow = 81 * row + 9 * col + value
		matrixRow++
		return matrixRow
	}

	private fun rowToCell(matrixRow: Int): IntArray {
		val matrixRowVal = matrixRow - 1
		val rowColVal = IntArray(3)
		rowColVal[0] = matrixRowVal / 81
		rowColVal[1] = matrixRowVal % 81 / 9
		rowColVal[2] = matrixRowVal % 9 + 1
		return rowColVal
	}

	/**
	 * Functions to move cyclically through matrix
	 */
	private fun moveLeft(j: Int, numCols: Int): Int = if (j - 1 < 0) numCols - 1 else j - 1

	private fun moveRight(j: Int, numCols: Int): Int = (j + 1) % numCols

	private fun moveUp(i: Int, numRows: Int): Int = if (i - 1 < 0) numRows - 1 else i - 1

	private fun moveDown(i: Int, numRows: Int): Int = (i + 1) % numRows

	/**
	 * Unlinks node from linked list
	 */
	private fun cover(node: Node) {
		val colNode = node.columnHeader
		colNode.left.right = colNode.right
		colNode.right.left = colNode.left
		var rowNode = colNode.down
		while (rowNode !== colNode) {
			var rightNode = rowNode.right
			while (rightNode !== rowNode) {
				rightNode.up.down = rightNode.down
				rightNode.down.up = rightNode.up
				rightNode.columnHeader.count--
				rightNode = rightNode.right
			}
			rowNode = rowNode.down
		}
	}

	private fun uncover(node: Node) {
		val colNode = node.columnHeader
		var upNode = colNode.up
		while (upNode !== colNode) {
			var leftNode = upNode.left
			while (leftNode !== upNode) {
				leftNode.up.down = leftNode
				leftNode.down.up = leftNode
				leftNode.columnHeader.count++
				leftNode = leftNode.left
			}
			upNode = upNode.up
		}
		colNode.left.right = colNode
		colNode.right.left = colNode
	}

	/**
	 * Returns column node with lowest # of nodes
	 */
	private fun chooseLeastCountNode(): Node {
		var bestNode = mHead.right
		var currentNode = bestNode.right
		while (currentNode !== mHead) {
			if (currentNode.count < bestNode.count) {
				bestNode = currentNode
			}
			currentNode = currentNode.right
		}
		return bestNode
	}
}

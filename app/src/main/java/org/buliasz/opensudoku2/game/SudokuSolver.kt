package org.buliasz.opensudoku2.game

import java.util.Arrays

class SudokuSolver {
    private val numRows = 9
    private val numCols = 9
    private val numValues = 9
    private val numConstraints = 4
    private val numCells = numRows * numCols
    private lateinit var mConstraintMatrix: Array<IntArray>
    private lateinit var mLinkedList: Array<Array<Node?>>
    private var mHead: Node? = null
    private var mSolution: ArrayList<Node?> = ArrayList()

    init {
        initializeConstraintMatrix()
        initializeLinkedList()
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
                val `val` = cell.value
                if (!cell.isEditable) {
                    val matrixRow = cellToRow(row, col, `val` - 1)
                    val matrixCol = 9 * row + col // calculates column of node based on cell constraint
                    val rowNode = mLinkedList[matrixRow][matrixCol]
                    var rightNode = rowNode
                    do {
                        cover(rightNode)
                        rightNode = rightNode!!.right
                    } while (rightNode !== rowNode)
                }
            }
        }
    }

    fun solve(): ArrayList<IntArray> {
        mSolution = dlx()
        val finalValues = ArrayList<IntArray>()
        for (node in mSolution) {
            val matrixRow = node!!.rowID
            val rowColVal = rowToCell(matrixRow)
            finalValues.add(rowColVal)
        }
        return finalValues
    }

    /* ---------------FUNCTIONS TO IMPLEMENT SOLVER--------------- */
    private fun initializeConstraintMatrix() {
        // add row of 1's for column headers
        mConstraintMatrix = Array(numRows * numCols * numValues + 1) { IntArray(numCells * numConstraints) }
        Arrays.fill(mConstraintMatrix[0], 1)

        // calculate column where constraint will go
        val rowShift = numCells
        val colShift = numCells * 2
        val blockShift = numCells * 3
        var cellColumn = 0
        var rowColumn = 0
        var colColumn = 0
        var blockColumn = 0
        for (row in 0..<numRows) {
            for (col in 0..<numCols) {
                for (`val` in 0..<numValues) {
                    val matrixRow = cellToRow(row, col, `val`)

                    // cell constraint
                    mConstraintMatrix[matrixRow][cellColumn] = 1

                    // row constraint
                    mConstraintMatrix[matrixRow][rowColumn + rowShift] = 1
                    rowColumn++

                    // col constraint
                    mConstraintMatrix[matrixRow][colColumn + colShift] = 1
                    colColumn++

                    // block constraint
                    mConstraintMatrix[matrixRow][blockColumn + blockShift] = 1
                    blockColumn++
                }
                cellColumn++
                rowColumn -= 9
                if (col % 3 != 2) {
                    blockColumn -= 9
                }
            }
            rowColumn += 9
            colColumn -= 81
            if (row % 3 != 2) {
                blockColumn -= 27
            }
        }
    }

    private fun initializeLinkedList() {
        mLinkedList = Array(numRows * numCols * numValues + 1) { arrayOfNulls(numCells * numConstraints) }
        mHead = Node()
        val rows = mLinkedList.size
        val cols = mLinkedList[0].size

        // create node for each 1 in constraint matrix
        for (i in 0..<rows) {
            for (j in 0..<cols) {
                if (mConstraintMatrix[i][j] == 1) {
                    mLinkedList[i][j] = Node()
                }
            }
        }

        // link nodes in mLinkedList
        for (i in 0..<rows) {
            for (j in 0..<cols) {
                if (mConstraintMatrix[i][j] == 1) {
                    var a: Int
                    var b: Int

                    // link left
                    a = i
                    b = j
                    do {
                        b = moveLeft(b, cols)
                    } while (mConstraintMatrix[a][b] != 1)
                    mLinkedList[i][j]!!.left = mLinkedList[a][b]

                    // link right
                    a = i
                    b = j
                    do {
                        b = moveRight(b, cols)
                    } while (mConstraintMatrix[a][b] != 1)
                    mLinkedList[i][j]!!.right = mLinkedList[a][b]

                    // link up
                    a = i
                    b = j
                    do {
                        a = moveUp(a, rows)
                    } while (mConstraintMatrix[a][b] != 1)
                    mLinkedList[i][j]!!.up = mLinkedList[a][b]

                    // link up
                    a = i
                    b = j
                    do {
                        a = moveDown(a, rows)
                    } while (mConstraintMatrix[a][b] != 1)
                    mLinkedList[i][j]!!.down = mLinkedList[a][b]

                    // initialize remaining node info
                    mLinkedList[i][j]!!.columnHeader = mLinkedList[0][j]
                    mLinkedList[i][j]!!.rowID = i
                    mLinkedList[i][j]!!.colID = j
                }
            }
        }

        // link head node
        mHead!!.right = mLinkedList[0][0]
        mHead!!.left = mLinkedList[0][cols - 1]
        mLinkedList[0][0]!!.left = mHead
        mLinkedList[0][cols - 1]!!.right = mHead
    }

    /**
     * Dancing links algorithm
     *
     * @return array of solution nodes or empty array if no solution exists
     */
    private fun dlx(): ArrayList<Node?> {
        if (mHead!!.right === mHead) {
            return mSolution
        }
        var colNode = chooseColumn()
        cover(colNode)
        var rowNode: Node?
        rowNode = colNode!!.down
        while (rowNode !== colNode) {
            mSolution.add(rowNode)
            var rightNode = rowNode!!.right
            while (rightNode !== rowNode) {
                cover(rightNode)
                rightNode = rightNode!!.right
            }
            val tempSolution = dlx()
            if (tempSolution.isNotEmpty()) {
                return tempSolution
            }

            // undo operations and try the next row
            mSolution.removeAt(mSolution.size - 1)
            colNode = rowNode.columnHeader
            var leftNode: Node?
            leftNode = rowNode.left
            while (leftNode !== rowNode) {
                uncover(leftNode)
                leftNode = leftNode!!.left
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
     * @param `val`             0-8 index (representing values 1-9)
     * @return row in mConstraintMatrix corresponding to cell indices and value
     */
    private fun cellToRow(row: Int, col: Int, `val`: Int): Int {
        var matrixRow = 81 * row + 9 * col + `val`
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
    private fun cover(node: Node?) {
        val colNode = node!!.columnHeader
        colNode!!.left!!.right = colNode.right
        colNode.right!!.left = colNode.left
        var rowNode: Node?
        rowNode = colNode.down
        while (rowNode !== colNode) {
            var rightNode: Node?
            rightNode = rowNode!!.right
            while (rightNode !== rowNode) {
                rightNode!!.up!!.down = rightNode.down
                rightNode.down!!.up = rightNode.up
                rightNode.columnHeader!!.count--
                rightNode = rightNode.right
            }
            rowNode = rowNode.down
        }
    }

    private fun uncover(node: Node?) {
        val colNode = node!!.columnHeader
        var upNode: Node?
        upNode = colNode!!.up
        while (upNode !== colNode) {
            var leftNode: Node?
            leftNode = upNode!!.left
            while (leftNode !== upNode) {
                leftNode!!.up!!.down = leftNode
                leftNode.down!!.up = leftNode
                leftNode.columnHeader!!.count++
                leftNode = leftNode.left
            }
            upNode = upNode.up
        }
        colNode.left!!.right = colNode
        colNode.right!!.left = colNode
    }

    /**
     * Returns column node with lowest # of nodes
     */
    private fun chooseColumn(): Node? {
        var bestNode: Node? = null
        var lowestNum = 100000
        var currentNode = mHead!!.right
        while (currentNode !== mHead) {
            if (currentNode!!.count < lowestNum) {
                bestNode = currentNode
                lowestNum = currentNode.count
            }
            currentNode = currentNode.right
        }
        return bestNode
    }
}

package org.buliasz.opensudoku2.game

class Node {
    var left: Node?
    var right: Node?
    var up: Node?
    var down: Node? = null
    var columnHeader: Node? = null
    var rowID: Int
    var colID: Int
    var count: Int

    init {
        up = down
        right = up
        left = right
        colID = -1
        rowID = colID
        count = 0
    }
}

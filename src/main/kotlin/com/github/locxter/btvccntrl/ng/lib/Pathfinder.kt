package com.github.locxter.btvccntrl.ng.lib

import com.github.locxter.btvccntrl.ng.model.EDirection
import com.github.locxter.btvccntrl.ng.model.Map
import com.github.locxter.btvccntrl.ng.model.Path
import com.github.locxter.btvccntrl.ng.model.Point
import kotlin.math.*

class Pathfinder() {
    private val directions = arrayOf(EDirection.DIRECTION_UP, EDirection.DIRECTION_RIGHT, EDirection.DIRECTION_DOWN, EDirection.DIRECTION_LEFT)
    var map = Map()
        set(value) {
            val simplifiedMap = Map()
            val uniqueMap = Map()
            // Simplify map
            for (point in value.points) {
                simplifiedMap.points.add(
                    Point(
                        round(point.x / simplificationFactor.toDouble()).roundToInt(),
                        round(point.y / simplificationFactor.toDouble()).roundToInt(),
                    )
                )
            }
            // Filter newly created duplicates
            for (simplifiedPoint in simplifiedMap.points) {
                var unique = true
                for (uniquePoint in uniqueMap.points) {
                    if (simplifiedPoint.x == uniquePoint.x && simplifiedPoint.y == uniquePoint.y) {
                        unique = false
                    }
                }
                if (unique) {
                    uniqueMap.points.add(simplifiedPoint)
                }
            }
            // Determine value ranges
            xMin = 0
            xMax = 0
            yMin = 0
            yMax = 0
            for (point in uniqueMap.points) {
                if (point.x < xMin) {
                    xMin = point.x
                } else if (point.x > xMax) {
                    xMax = point.y
                }
                if (point.y < yMin) {
                    yMin = point.y
                } else if (point.y > yMax) {
                    yMax = point.y
                }
            }
            // Move all the data to positive values
            for (i in uniqueMap.points.indices) {
                val oldPoint = uniqueMap.points[i]
                uniqueMap.points[i] = Point(oldPoint.x - xMin, oldPoint.y - yMin)
            }
            field = uniqueMap
        }
    private var xMin = -1
    private var xMax = -1
    private var yMin = -1
    private var yMax = -1
    var simplificationFactor = 1
        set(value) {
            field = min(max(value, 1), 1000)
        }

    constructor(map: Map) : this() {
        this.map = map
    }

    constructor(simplificationFactor: Int) : this() {
        this.simplificationFactor = simplificationFactor
    }

    constructor(map: Map, simplificationFactor: Int) : this() {
        this.map = map
        this.simplificationFactor = simplificationFactor
    }

    // Method to find the shortest path from source to target
    fun findPath(source: Point, target: Point): Path {
        var currentNode = Node()
        val openList = mutableListOf<Node>()
        val closedList = mutableListOf<Node>()
        val path = Path()
        val simplifiedPath = Path()
        // Check for valid source and target
        val sourceAdjusted = Point(
            round((source.x / simplificationFactor.toDouble()) - xMin).roundToInt(),
            round((source.y / simplificationFactor.toDouble()) - yMin).roundToInt()
        )
        val targetAdjusted = Point(
            round((target.x / simplificationFactor.toDouble()) - xMin).roundToInt(),
            round((target.y / simplificationFactor.toDouble()) - yMin).roundToInt()
        )
        if (detectCollision(sourceAdjusted) || detectCollision(targetAdjusted)) {
            return simplifiedPath
        }
        // Initialize
        openList.add(Node(sourceAdjusted))
        // Find shortest path
        while (openList.isNotEmpty()) {
            currentNode = openList.first()
            // Find best node to further investigate
            for (node in openList) {
                if (node.getTotalCost() <= currentNode.getTotalCost()) {
                    currentNode = node
                }
            }
            // Check for success
            if (currentNode.point.x == targetAdjusted.x && currentNode.point.y == targetAdjusted.y) {
                break
            }
            // Move current node to closed list
            closedList.add(currentNode)
            openList.remove(currentNode)
            // Add successors to open list
            for (i in 0..3) {
                val successorPoint = Point(
                    currentNode.point.x + directions[i].point.x,
                    currentNode.point.y + directions[i].point.y
                )
                val successorPastCost = currentNode.pastCost + 1
                // Skip invalid successor
                if (detectCollision(successorPoint) || findNodeByPoint(closedList, successorPoint) != null) {
                    continue
                }
                // Add or update successor
                val existingSuccessor = findNodeByPoint(openList, successorPoint)
                if (existingSuccessor == null) {
                    val successor = Node(
                        successorPoint,
                        successorPastCost,
                        abs(successorPoint.x - targetAdjusted.x) + abs(successorPoint.y - targetAdjusted.y),
                        currentNode
                    )
                    openList.add(successor)
                } else if (successorPastCost < existingSuccessor.pastCost) {
                    existingSuccessor.predecessor = currentNode.copy()
                    existingSuccessor.pastCost = successorPastCost
                }
            }
        }
        // Reconstruct path
        while (true) {
            path.points.add(
                0, Point(
                    (currentNode.point.x + xMin) * simplificationFactor,
                    (currentNode.point.y + yMin) * simplificationFactor
                )
            )
            currentNode = currentNode.predecessor ?: break
        }
        // Simplify path to corners
        for (i in 1 until path.points.size - 1) {
            if ((path.points[i].x == path.points[i - 1].x && path.points[i].x != path.points[i + 1].x) ||
                (path.points[i].y == path.points[i - 1].y && path.points[i].y != path.points[i + 1].y)
            ) {
                simplifiedPath.points.add(path.points[i])
            }
        }
        simplifiedPath.points.add(path.points.last())
        // Check for successful pathfinding
        if (simplifiedPath.points.last().x != (targetAdjusted.x + xMin) * simplificationFactor ||
            simplifiedPath.points.last().y != (targetAdjusted.y + yMin) * simplificationFactor
        ) {
            simplifiedPath.points.clear()
        }
        return simplifiedPath
    }

    // Helper class to store costs and predecessor alongside a point
    private data class Node(
        var point: Point = Point(),
        var pastCost: Int = 0,
        var futureCost: Int = 0,
        var predecessor: Node? = null
    ) {

        fun getTotalCost(): Int {
            return pastCost + futureCost
        }
    }

    // Helper method to detect a collision
    private fun detectCollision(point: Point): Boolean {
        val collisionBoxSize = ceil(250.0 / simplificationFactor).roundToInt()
        var returnValue = false
        if (point.x < 0 || point.x > abs(xMin) + abs(xMax) || point.y < 0 || point.y > abs(yMin) + abs(yMax)) {
            returnValue = true
        }
        for (otherPoint in map.points) {
            if (otherPoint.x >= point.x - collisionBoxSize && otherPoint.x <= point.x + collisionBoxSize &&
                otherPoint.y >= point.y - collisionBoxSize && otherPoint.y <= point.y + collisionBoxSize
            ) {
                returnValue = true
            }
        }
        return returnValue
    }

    // Helper method to find a node within a list by its point
    private fun findNodeByPoint(list: List<Node>, point: Point): Node? {
        return list.find { it.point.x == point.x && it.point.y == point.y }
    }
}

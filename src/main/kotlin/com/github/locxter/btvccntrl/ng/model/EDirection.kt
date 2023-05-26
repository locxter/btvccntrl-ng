package com.github.locxter.btvccntrl.ng.model

enum class EDirection(val point: Point) {
    DIRECTION_UP(Point(0,1)),
    DIRECTION_RIGHT(Point(1,0)),
    DIRECTION_DOWN(Point(0,-1)),
    DIRECTION_LEFT(Point(-1,0)),
}

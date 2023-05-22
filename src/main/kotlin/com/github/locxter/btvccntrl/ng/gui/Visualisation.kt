package com.github.locxter.btvccntrl.ng.gui

import com.github.locxter.btvccntrl.ng.model.Botvac
import com.github.locxter.btvccntrl.ng.model.Map
import com.github.locxter.btvccntrl.ng.model.Point
import com.github.locxter.btvccntrl.ng.model.Scan
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import kotlin.math.*


class Visualisation() : JComponent() {
    private var scalingRatio = 0.0
    private var showStartScreen = true
    var botvac: Botvac = Botvac()
        set(value) {
            val valueCopy = value.copy(
                scan = Scan(value.scan.points.map { Point(it.x, it.y) }.toMutableList()),
                map = Map(value.map.points.map { Point(it.x, it.y) }.toMutableList())
            )
            if (valueCopy.map.points.isNotEmpty()) {
                showStartScreen = false
                xMin = 0
                xMax = 0
                yMin = 0
                yMax = 0
                xRange = 0
                yRange = 0
                maxRange = 0
                // Determine value ranges
                for (point in valueCopy.map.points) {
                    if (point.x < xMin) {
                        xMin = point.x
                    } else if (point.x > xMax) {
                        xMax = point.x
                    }
                    if (point.y < yMin) {
                        yMin = point.y
                    } else if (point.y > yMax) {
                        yMax = point.y
                    }
                }
                xRange = abs(xMin) + abs(xMax)
                yRange = abs(yMin) + abs(yMax)
                maxRange = max(xRange, yRange)
                // Move all the data to positive values
                for (i in valueCopy.map.points.indices) {
                    val oldPoint = valueCopy.map.points[i]
                    valueCopy.map.points[i] = Point(oldPoint.x - xMin, oldPoint.y - yMin)
                }
                valueCopy.x -= xMin
                valueCopy.y -= yMin
            } else {
                showStartScreen = true
            }
            field = valueCopy
            repaint()
        }
    var clickX: Int = -1
    var clickY: Int = -1
    private var xMin: Int = 0
    private var xMax: Int = 0
    private var yMin: Int = 0
    private var yMax: Int = 0
    private var xRange: Int = 0
    private var yRange: Int = 0
    private var maxRange: Int = 0

    constructor(botvac: Botvac) : this() {
        this.botvac = botvac
    }

    init {
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (!showStartScreen) {
                    val xOffset = ((width - getScaledValue(xRange)) / 2.0).roundToInt()
                    val yOffset = ((height - getScaledValue(yRange)) / 2.0).roundToInt()
                    if (event.x > xOffset && event.x < width - xOffset &&
                        event.y > yOffset && event.y < height - yOffset
                    ) {
                        clickX = (((event.x - xOffset) * (1 / scalingRatio)) + xMin).roundToInt()
                        clickY = (((height - (yOffset + event.y)) * (1 / scalingRatio)) + yMin).roundToInt()
                    }

                }
            }
        })
    }

    // Method to draw the component
    override fun paintComponent(context: Graphics) {
        // Clear the component
        super.paintComponent(context)
        val context2d = context as Graphics2D
        context2d.color = Color(60, 63, 65)
        context2d.fillRect(0, 0, width, height)
        if (showStartScreen) {
            // Draw a start screen if no visualisation has been requested yet
            val font = Font(Font.SANS_SERIF, Font.PLAIN, 24)
            val metrics = context2d.getFontMetrics(font)
            val message = "Connect to robot vacuum to see the visualisation."
            context2d.font = font
            context2d.color = Color(255, 255, 255)
            context2d.drawString(
                message,
                ((width - metrics.stringWidth(message)) / 2.toDouble()).roundToInt(),
                (((height - metrics.height) / 2.toDouble()) + metrics.ascent).roundToInt()
            )
        } else {
            // Calculate the scaling ratio and center the canvas
            if (width.toDouble() / height > xRange.toDouble() / yRange) {
                scalingRatio = height / yRange.toDouble()
                context2d.translate(((width - getScaledValue(xRange).toDouble()) / 2).roundToInt(), 0)
            } else {
                scalingRatio = width / xRange.toDouble()
                context2d.translate(0, ((height - getScaledValue(yRange).toDouble()) / 2).roundToInt())
            }
            // Draw the points
            context2d.color = Color(255, 255, 255)
            var radius = max(getScaledValue((maxRange * 0.005).roundToInt()), 2)
            for (point in botvac.map.points) {
                context2d.fillOval(
                    getScaledValue(point.x) - radius,
                    getScaledValue(yRange - point.y) - radius,
                    radius * 2,
                    radius * 2
                )
            }
            // Draw the robot and it's movement direction
            context2d.color = Color(0, 255, 0)
            radius = max(getScaledValue((maxRange * 0.01).roundToInt()), 4)
            context2d.fillOval(
                getScaledValue(botvac.x) - radius,
                getScaledValue(yRange - botvac.y) - radius,
                radius * 2,
                radius * 2
            )
            context2d.stroke = BasicStroke(max(getScaledValue((maxRange * 0.005).roundToInt()), 2).toFloat())
            context2d.drawLine(
                getScaledValue(botvac.x),
                getScaledValue(yRange - botvac.y),
                getScaledValue(botvac.x) + (max(
                    getScaledValue((maxRange * 0.05).roundToInt()),
                    20
                ) * sin(botvac.angle * (PI / 180))).roundToInt(),
                getScaledValue(yRange - botvac.y) + (max(
                    getScaledValue((maxRange * 0.05).roundToInt()),
                    20
                ) * cos((180 - botvac.angle) * (PI / 180))).roundToInt()
            )
        }
    }

    // Helper method to transform an unscaled value to a scaled one
    private fun getScaledValue(unscaledValue: Int): Int {
        return (unscaledValue * scalingRatio).roundToInt()
    }
}

package com.github.locxter.btvccntrl.ng

import com.formdev.flatlaf.FlatDarkLaf
import com.github.locxter.btvccntrl.ng.gui.Visualisation
import com.github.locxter.btvccntrl.ng.lib.BotvacController
import com.github.locxter.btvccntrl.ng.lib.Pathfinder
import com.github.locxter.btvccntrl.ng.model.Botvac
import com.github.locxter.btvccntrl.ng.model.Map
import com.github.locxter.btvccntrl.ng.model.Path
import com.github.locxter.btvccntrl.ng.model.Point
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    // Check for the right number of command line arguments
    if (args.size == 2) {
        // Communication and navigation related variables
        val useNetwork = args[0] == "network"
        val device = args[1]
        val speed = 150
        val botvacController = BotvacController(50, 0.01)
        var botvac = Botvac()
        val pathfinder = Pathfinder(80)
        var path = Path()
        // Set a pleasing LaF
        try {
            UIManager.setLookAndFeel(FlatDarkLaf())
        } catch (exception: Exception) {
            println("Failed to initialize LaF.")
        }
        // UI components
        val frame = JFrame("btvccntrl-ng")
        val panel = JPanel()
        val constraints = GridBagConstraints()
        val connectButton = JButton("Connect")
        val pitchLabel = JLabel("Pitch:")
        val pitchData = JLabel()
        val rollLabel = JLabel("Roll:")
        val rollData = JLabel()
        val chargeLabel = JLabel("Charge:")
        val chargeData = JLabel()
        val leftMagnetLabel = JLabel("Left magnet:")
        val leftMagnetData = JLabel()
        val rightMagnetLabel = JLabel("Right magnet:")
        val rightMagnetData = JLabel()
        val wallLabel = JLabel("Wall:")
        val wallData = JLabel()
        val leftDropLabel = JLabel("Left drop:")
        val leftDropData = JLabel()
        val rightDropLabel = JLabel("Right drop:")
        val rightDropData = JLabel()
        val leftWheelLabel = JLabel("Left wheel:")
        val leftWheelData = JLabel()
        val rightWheelLabel = JLabel("Right wheel:")
        val rightWheelData = JLabel()
        val leftFrontLabel = JLabel("Left front:")
        val leftFrontData = JLabel()
        val rightFrontLabel = JLabel("Right front:")
        val rightFrontData = JLabel()
        val leftSideLabel = JLabel("Left side:")
        val leftSideData = JLabel()
        val rightSideLabel = JLabel("Right side:")
        val rightSideData = JLabel()
        val visualisation = Visualisation()
        val forwardButton = JButton("Forward")
        val brushLabel = JLabel("Brush:")
        val brushInput = JSpinner(SpinnerNumberModel(0, 0, 10000, 10))
        val leftButton = JButton("Left")
        val rightButton = JButton("Right")
        val vacuumLabel = JLabel("Vacuum:")
        val vacuumInput = JSpinner(SpinnerNumberModel(0, 0, 100, 1))
        val backwardButton = JButton("Backward")
        val sideBrushLabel = JLabel("Side brush:")
        val sideBrushInput = JComboBox(arrayOf("Off", "On"))
        val aboutLabel = JLabel("2023 locxter")
        // Add functions to the buttons
        connectButton.addActionListener {
            if (!botvacController.connected) {
                botvacController.connect(device, useNetwork)
                connectButton.text = "Disconnect"
            } else {
                if (botvac.map.points.isNotEmpty()) {
                    var counter = 0
                    var fullFilename = "map-$counter.csv"
                    while (File(fullFilename).exists()) {
                        counter++
                        fullFilename = "map-$counter.csv"
                    }
                    val bufferedWriter = BufferedWriter(FileWriter(fullFilename))
                    bufferedWriter.write("X:, Y:")
                    bufferedWriter.newLine()
                    for (point in botvac.map.points) {
                        bufferedWriter.write("${point.x}, ${point.y}")
                        bufferedWriter.newLine()
                    }
                    bufferedWriter.close()
                }
                botvacController.disconnect()
                botvac = Botvac()
                path = Path()
                visualisation.botvac = botvac
                connectButton.text = "Connect"
            }
        }
        forwardButton.addActionListener {
            if (botvacController.connected) {
                botvacController.moveRobot(500, speed)
                botvac = botvacController.botvac
                if (botvac.map.points.isNotEmpty()) {
                    visualisation.botvac = botvac
                }
            }
        }
        brushInput.addChangeListener {
            if (botvacController.connected) {
                botvacController.controlBrush(brushInput.value as Int)
            }
        }
        leftButton.addActionListener {
            if (botvacController.connected) {
                botvacController.rotateRobot(-90, speed)
                botvac = botvacController.botvac
                if (botvac.map.points.isNotEmpty()) {
                    visualisation.botvac = botvac
                }
            }
        }
        rightButton.addActionListener {
            if (botvacController.connected) {
                botvacController.rotateRobot(90, speed)
                botvac = botvacController.botvac
                if (botvac.map.points.isNotEmpty()) {
                    visualisation.botvac = botvac
                }
            }
        }
        vacuumInput.addChangeListener {
            if (botvacController.connected) {
                botvacController.controlVacuum(vacuumInput.value as Int)
            }
        }
        backwardButton.addActionListener {
            if (botvacController.connected) {
                botvacController.moveRobot(-500, speed)
                botvac = botvacController.botvac
                if (botvac.map.points.isNotEmpty()) {
                    visualisation.botvac = botvac
                }
            }
        }
        sideBrushInput.addActionListener {
            if (botvacController.connected) {
                botvacController.controlSideBrush(sideBrushInput.selectedIndex != 0)
            }
        }
        // Create a background function for updating the map and it's visualisation as well as handling navigation
        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleAtFixedRate({
            if (botvacController.connected) {
                // Update visualisation and other data
                botvacController.updateAccelerometer()
                botvac = botvacController.botvac
                pitchData.text = botvac.pitch.toString()
                rollData.text = botvac.roll.toString()
                botvacController.updateCharge()
                botvac = botvacController.botvac
                chargeData.text = botvac.charge.toString()
                botvacController.updateAnalogSensors()
                botvac = botvacController.botvac
                leftMagnetData.text = botvac.leftMagnetStrength.toString()
                rightMagnetData.text = botvac.rightMagnetStrength.toString()
                wallData.text = botvac.wallDistance.toString()
                leftDropData.text = botvac.leftDropDistance.toString()
                rightDropData.text = botvac.rightDropDistance.toString()
                botvacController.updateDigitalSensors()
                botvac = botvacController.botvac
                leftWheelData.text = botvac.leftWheelExtended.toString()
                rightWheelData.text = botvac.rightWheelExtended.toString()
                leftFrontData.text = botvac.leftFrontBumperPressed.toString()
                rightFrontData.text = botvac.rightFrontBumperPressed.toString()
                leftSideData.text = botvac.leftSideBumperPressed.toString()
                rightSideData.text = botvac.rightSideBumperPressed.toString()
                botvacController.updateLidar()
                botvac = botvacController.botvac
                if (botvac.map.points.isNotEmpty()) {
                    visualisation.botvac = botvac
                }
                // Plan path if wanted
                if (botvac.map.points.isNotEmpty() && path.points.isEmpty() && visualisation.clickX != null && visualisation.clickY != null) {
                    pathfinder.map = Map(botvac.map.points.map { Point(it.x, it.y) }.toMutableList())
                    path = pathfinder.findPath(
                        Point(botvac.x, botvac.y),
                        Point(visualisation.clickX!!, visualisation.clickY!!)
                    )
                    // Show a confirmation dialog
                    if (path.points.isNotEmpty()) {
                        val response = JOptionPane.showOptionDialog(
                            frame,
                            "Follow generated path autonomously?",
                            "Pathfinder",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            null
                        )
                        if (response != JOptionPane.OK_OPTION) {
                            path.points.clear()
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                            frame, "No path found", "Pathfinder",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                    visualisation.clickX = null
                    visualisation.clickY = null
                }
                // Follow path if possible
                if (path.points.isNotEmpty()) {
                    for (point in path.points) {
                        botvacController.moveToPoint(point, speed)
                        botvac = botvacController.botvac
                        visualisation.botvac = botvac
                    }
                    path.points.clear()
                }
            }
        }, 0, 5, TimeUnit.SECONDS)
        // Create the main panel
        panel.border = EmptyBorder(5, 5, 5, 5)
        panel.layout = GridBagLayout()
        constraints.insets = Insets(5, 5, 5, 5)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.gridwidth = 4
        panel.add(connectButton, constraints)
        constraints.fill = GridBagConstraints.RELATIVE
        constraints.gridx = 0
        constraints.gridy = 1
        constraints.gridwidth = 1
        panel.add(pitchLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 1
        panel.add(pitchData, constraints)
        constraints.gridx = 2
        constraints.gridy = 1
        panel.add(rollLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 1
        panel.add(rollData, constraints)
        constraints.gridx = 0
        constraints.gridy = 2
        panel.add(chargeLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 2
        panel.add(chargeData, constraints)
        constraints.gridx = 2
        constraints.gridy = 2
        panel.add(leftMagnetLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 2
        panel.add(leftMagnetData, constraints)
        constraints.gridx = 0
        constraints.gridy = 3
        panel.add(rightMagnetLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 3
        panel.add(rightMagnetData, constraints)
        constraints.gridx = 2
        constraints.gridy = 3
        panel.add(wallLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 3
        panel.add(wallData, constraints)
        constraints.gridx = 0
        constraints.gridy = 4
        panel.add(leftDropLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 4
        panel.add(leftDropData, constraints)
        constraints.gridx = 2
        constraints.gridy = 4
        panel.add(rightDropLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 4
        panel.add(rightDropData, constraints)
        constraints.gridx = 0
        constraints.gridy = 5
        panel.add(leftWheelLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 5
        panel.add(leftWheelData, constraints)
        constraints.gridx = 2
        constraints.gridy = 5
        panel.add(rightWheelLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 5
        panel.add(rightWheelData, constraints)
        constraints.gridx = 0
        constraints.gridy = 6
        panel.add(leftFrontLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 6
        panel.add(leftFrontData, constraints)
        constraints.gridx = 2
        constraints.gridy = 6
        panel.add(rightFrontLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 6
        panel.add(rightFrontData, constraints)
        constraints.gridx = 0
        constraints.gridy = 7
        panel.add(leftSideLabel, constraints)
        constraints.gridx = 1
        constraints.gridy = 7
        panel.add(leftSideData, constraints)
        constraints.gridx = 2
        constraints.gridy = 7
        panel.add(rightSideLabel, constraints)
        constraints.gridx = 3
        constraints.gridy = 7
        panel.add(rightSideData, constraints)
        constraints.fill = GridBagConstraints.BOTH
        constraints.weighty = 1.0
        constraints.gridx = 0
        constraints.gridy = 8
        constraints.gridwidth = 4
        panel.add(visualisation, constraints)
        constraints.fill = GridBagConstraints.RELATIVE
        constraints.weighty = 0.0
        constraints.gridx = 1
        constraints.gridy = 9
        constraints.gridwidth = 1
        constraints.gridheight = 2
        panel.add(forwardButton, constraints)
        constraints.gridx = 3
        constraints.gridy = 9
        constraints.gridheight = 1
        panel.add(brushLabel, constraints)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridx = 3
        constraints.gridy = 10
        panel.add(brushInput, constraints)
        constraints.fill = GridBagConstraints.RELATIVE
        constraints.gridx = 0
        constraints.gridy = 11
        constraints.gridheight = 2
        panel.add(leftButton, constraints)
        constraints.gridx = 2
        constraints.gridy = 11
        panel.add(rightButton, constraints)
        constraints.gridx = 3
        constraints.gridy = 11
        constraints.gridheight = 1
        panel.add(vacuumLabel, constraints)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridx = 3
        constraints.gridy = 12
        panel.add(vacuumInput, constraints)
        constraints.fill = GridBagConstraints.RELATIVE
        constraints.gridx = 1
        constraints.gridy = 13
        constraints.gridheight = 2
        panel.add(backwardButton, constraints)
        constraints.gridx = 3
        constraints.gridy = 13
        constraints.gridheight = 1
        panel.add(sideBrushLabel, constraints)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridx = 3
        constraints.gridy = 14
        panel.add(sideBrushInput, constraints)
        constraints.fill = GridBagConstraints.RELATIVE
        constraints.gridx = 0
        constraints.gridy = 15
        constraints.gridwidth = 4
        panel.add(aboutLabel, constraints)
        // Create the main window
        frame.size = Dimension(640, 640)
        frame.minimumSize = Dimension(480, 480)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(panel)
        frame.isVisible = true
    } else {
        // Throw an error on invalid number of command line arguments
        println("Wrong number of arguments. Two arguments containing the connection mode (serial or network) and device expected.")
        println("Example: serial /dev/ttyACM0")
        exitProcess(1)
    }
}

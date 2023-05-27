package com.github.locxter.btvccntrl.ng.lib

import com.fazecast.jSerialComm.SerialPort
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import com.github.locxter.btvccntrl.ng.model.Botvac
import com.github.locxter.btvccntrl.ng.model.EDirection
import com.github.locxter.btvccntrl.ng.model.Point
import java.lang.Thread.sleep
import kotlin.math.*

class BotvacController() {
    var connected: Boolean = false
        private set
    var device: String = ""
        private set
    var useNetwork: Boolean = false
        private set
    private var serialPort: SerialPort = SerialPort.getCommPort("")
    var botvac: Botvac = Botvac()
    var minPointDistance: Int = 0
        set(value) {
            field = max(value, 0)
        }
    var inaccuracyFilterRatio: Double = 0.0
        set(value) {
            field = max(value, 0.0)
        }

    constructor(device: String, useNetwork: Boolean) : this() {
        connect(device, useNetwork)
    }

    constructor(minPointDistance: Int) : this() {
        this.minPointDistance = minPointDistance
    }

    constructor(inaccuracyFilterRatio: Double) : this() {
        this.inaccuracyFilterRatio = inaccuracyFilterRatio
    }

    constructor(minPointDistance: Int, inaccuracyFilterRatio: Double) : this() {
        this.minPointDistance = minPointDistance
        this.inaccuracyFilterRatio = inaccuracyFilterRatio
    }

    constructor(device: String, useNetwork: Boolean, minPointDistance: Int) : this() {
        connect(device, useNetwork)
        this.minPointDistance = minPointDistance
    }

    constructor(device: String, useNetwork: Boolean, inaccuracyFilterRatio: Double) : this() {
        connect(device, useNetwork)
        this.inaccuracyFilterRatio = inaccuracyFilterRatio
    }

    constructor(device: String, useNetwork: Boolean, minPointDistance: Int, inaccuracyFilterRatio: Double) : this() {
        connect(device, useNetwork)
        this.minPointDistance = minPointDistance
        this.inaccuracyFilterRatio = inaccuracyFilterRatio
    }

    fun connect(device: String, useNetwork: Boolean) {
        if (!connected) {
            if (!useNetwork) {
                serialPort = SerialPort.getCommPort(device)
                serialPort.openPort()
                serialPort.baudRate = 115200
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0)
            }
            connected = true
            this.device = device
            this.useNetwork = useNetwork
            sendCommand("TestMode On")
            sendCommand("SetLED BacklightOff")
            sendCommand("SetLED ButtonOff")
            sendCommand("SetLED SpotOff")
            sendCommand("SetLDSRotation On")
            sleep(1000)
        }

    }

    fun disconnect() {
        if (connected) {
            sendCommand("SetLED BacklightOn")
            sendCommand("SetLED ButtonGreen")
            sendCommand("SetLED SpotOn")
            sendCommand("SetLDSRotation Off")
            sendCommand("ClearFiles")
            sendCommand("TestMode Off")
            if (!useNetwork) {
                serialPort.closePort()
            }
            connected = false
            useNetwork = false
            device = ""
            serialPort = SerialPort.getCommPort("")
            botvac = Botvac()
        }
    }

    fun updateAccelerometer() {
        if (connected) {
            val lines = sendCommand("GetAccel").lines()
            for (i in lines.indices) {
                when (i) {
                    1 -> {
                        botvac.pitch = lines[i].substring(16).toDouble()
                    }

                    2 -> {
                        botvac.roll = lines[i].substring(15).toDouble()
                    }
                }
            }
        }
    }

    fun updateCharge() {
        if (connected) {
            val lines = sendCommand("GetCharger").lines()
            for (i in lines.indices) {
                when (i) {
                    1 -> {
                        botvac.charge = lines[i].substring(12).toInt()
                    }
                }
            }
        }
    }

    fun updateAnalogSensors() {
        if (connected) {
            val lines = sendCommand("GetAnalogSensors").lines().dropLast(1)
            for (i in lines.indices) {
                when (i) {
                    10 -> {
                        botvac.leftMagnetStrength = lines[i].substring(18).dropLast(1).toInt()
                    }

                    11 -> {
                        botvac.rightMagnetStrength = lines[i].substring(19).dropLast(1).toInt()
                    }

                    12 -> {
                        botvac.wallDistance = lines[i].substring(14).dropLast(1).toInt()
                    }

                    13 -> {
                        botvac.leftDropDistance = lines[i].substring(18).dropLast(1).toInt()
                    }

                    14 -> {
                        botvac.rightDropDistance = lines[i].substring(19).dropLast(1).toInt()
                    }
                }
            }
        }
    }

    fun updateDigitalSensors() {
        if (connected) {
            val lines = sendCommand("GetDigitalSensors").lines()
            for (i in lines.indices) {
                when (i) {
                    3 -> {
                        botvac.leftWheelExtended = lines[i].substring(25).toInt() != 0
                    }

                    4 -> {
                        botvac.rightWheelExtended = lines[i].substring(26).toInt() != 0
                    }

                    5 -> {
                        botvac.leftSideBumperPressed = lines[i].substring(9).toInt() != 0
                    }

                    6 -> {
                        botvac.leftFrontBumperPressed = lines[i].substring(10).toInt() != 0
                    }

                    8 -> {
                        botvac.rightSideBumperPressed = lines[i].substring(9).toInt() != 0
                    }

                    9 -> {
                        botvac.rightFrontBumperPressed = lines[i].substring(10).toInt() != 0
                    }
                }
            }
        }
    }

    fun updateLidar() {
        if (connected) {
            val lines = sendCommand("GetLDSScan").lines().drop(1).dropLast(2)
            botvac.scan.points.clear()
            for (i in lines.indices) {
                var unique = true
                val distance = lines[i].substringAfter(',').substringBefore(',').toInt()
                if (distance > 7500 || distance == 0) {
                    continue
                }
                botvac.scan.points.add(
                    Point(
                        (distance * sin(-i * (PI / 180))).roundToInt(),
                        (distance * cos(-i * (PI / 180))).roundToInt()
                    )
                )
                val point = Point(
                    (botvac.x + (distance * sin((-i + botvac.angle) * (PI / 180))) +
                            (-92.5 * sin(botvac.angle * (PI / 180)))).roundToInt(),
                    (botvac.y + (distance * cos((-i + botvac.angle) * (PI / 180))) +
                            (-92.5 * cos(botvac.angle * (PI / 180)))).roundToInt()
                )
                val inaccuracyFilter =
                    (sqrt((point.x - botvac.x.toDouble()).pow(2) +
                            (point.y - botvac.y.toDouble()).pow(2)) * inaccuracyFilterRatio).roundToInt()
                for (mapPoint in botvac.map.points) {
                    if (point.x >= mapPoint.x - (minPointDistance + inaccuracyFilter) &&
                        point.x <= mapPoint.x + (minPointDistance + inaccuracyFilter) &&
                        point.y >= mapPoint.y - (minPointDistance + inaccuracyFilter) &&
                        point.y <= mapPoint.y + (minPointDistance + inaccuracyFilter)
                    ) {
                        unique = false
                    }
                }
                if (unique) {
                    botvac.map.points.add(point)
                }
            }
        }
    }

    fun updateAll() {
        updateAccelerometer()
        updateCharge()
        updateAnalogSensors()
        updateDigitalSensors()
        updateLidar()
    }

    fun moveRobot(distance: Int, speed: Int) {
        if (connected) {
            val validDistance = min(max(distance, -10000), 10000)
            val validSpeed = min(max(speed, 1), 350)
            sendCommand("SetMotor LWheelDist $validDistance RWheelDist $validDistance Speed $validSpeed")
            botvac.x += (distance * sin(botvac.angle * (PI / 180))).roundToInt()
            botvac.y += (distance * cos(botvac.angle * (PI / 180))).roundToInt()
            sleep((ceil(abs(distance).toDouble() / validSpeed) * 1250).toLong())
        }
    }

    fun rotateRobot(angle: Int, speed: Int) {
        if (connected) {
            val validAngle = min(max(angle, -359), 359)
            val validSpeed = min(max(speed, 1), 350)
            val distance = (validAngle * ((250 * PI) / 360)).roundToInt()
            sendCommand("SetMotor LWheelDist $distance RWheelDist ${-1 * distance} Speed $validSpeed")
            botvac.angle = (botvac.angle + validAngle + 360) % 360
            sleep((ceil(abs(distance).toDouble() / validSpeed) * 1250).toLong())
        }
    }

    fun controlBrush(rpm: Int) {
        if (connected) {
            sendCommand("SetMotor Brush RPM ${min(max(rpm, 0), 10000)}")
        }
    }

    fun controlVacuum(dutyCycle: Int) {
        if (connected) {
            sendCommand("SetMotor VacuumOn VacuumSpeed ${min(max(dutyCycle, 0), 100)}")
        }
    }

    fun controlSideBrush(enable: Boolean) {
        if (connected) {
            if (enable) {
                sendCommand("SetMotor SideBrushOn SideBrushPower 5000")
            } else {
                sendCommand("SetMotor SideBrushOff")
            }
        }
    }

    fun moveToPoint(point: Point, speed: Int) {
        if (connected) {
            var direction = EDirection.DIRECTION_UP
            var distance = abs(point.y - botvac.y)
            if (point.x < botvac.x) {
                distance = abs(point.x - botvac.x)
                direction = EDirection.DIRECTION_LEFT
            } else if (point.x > botvac.x) {
                distance = abs(point.x - botvac.x)
                direction = EDirection.DIRECTION_RIGHT
            } else if (point.y < botvac.y) {
                direction = EDirection.DIRECTION_DOWN
            }
            if (botvac.angle != (direction.ordinal * 90)) {
                var angleToGo = (direction.ordinal * 90) - botvac.angle
                if (abs(angleToGo) == 270) {
                    angleToGo /= -3
                }
                rotateRobot(angleToGo, speed)
            }
            moveRobot(distance, speed)
        }
    }

    // Helper method to interact with the robot in a uniform way regardless of the connection mode
    private fun sendCommand(command: String): String {
        var rawResponse = ""
        if (useNetwork) {
            val (request, response, result) = Fuel.upload(device, parameters = listOf("command" to command))
                .authentication()
                .basic("btvcbrdg", "btvcbrdg")
                .responseString()
            when (result) {
                is Result.Failure -> {
                    throw result.getException()
                }

                is Result.Success -> {
                    rawResponse = response.body().asString("text/plain")
                }
            }
        } else {
            val bufferedWriter = serialPort.outputStream.bufferedWriter()
            bufferedWriter.write(command)
            bufferedWriter.newLine()
            bufferedWriter.close()
            val bufferedReader = serialPort.inputStream.bufferedReader()
            var line: String
            try {
                while (bufferedReader.readLine().also { line = it ?: "" } != null) {
                    rawResponse += line + '\n'
                }
            } catch (_: Exception) {
            }
            bufferedReader.close()
        }
        return rawResponse.substringAfter('\n').substringBeforeLast('\n')
    }
}

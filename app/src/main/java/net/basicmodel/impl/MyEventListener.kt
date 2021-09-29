package net.basicmodel.impl

import android.content.Context
import android.hardware.*
import kotlin.jvm.JvmOverloads
import net.basicmodel.utils.AverageList
import android.location.Location
import java.util.*

class MyEventListener @JvmOverloads constructor(context: Context, location: Location? = null) : SensorEventListener {

    interface CompassAssistantListener {
        fun onNewDegreesToNorth(degrees: Float)
        fun onNewSmoothedDegreesToNorth(degrees: Float)
        fun onCompassStopped()
        fun onCompassStarted()
        fun updateBearingText(bearing: String?)
    }

    private val sensorManager: SensorManager
    private val sensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var currentDegree = 0f
    private var currentSmoothedDegree = 0f
    private var declination = 0.0f
    private var isStarted = false
    private var dataList = AverageList(10)
    private var lastAccelerometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometer = FloatArray(3)
    private val magnetSensor: Sensor
    private val listeners: MutableList<CompassAssistantListener> = ArrayList()
    val isSupported: Boolean
        get() {
            val sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
            return sensors.size > 0
        }

    fun addListener(listener: CompassAssistantListener) {
        listeners.add(listener)
    }

    fun start() {
        if (!isStarted) {
            sensorManager.registerListener(
                this@MyEventListener, sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            sensorManager.registerListener(
                this@MyEventListener, magnetSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            for (listener in listeners) {
                listener.onCompassStarted()
            }
            isStarted = true
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this, sensor)
        sensorManager.unregisterListener(this, magnetSensor)
        lastAccelerometer = FloatArray(3)
        lastMagnetometer = FloatArray(3)
        lastAccelerometerSet = false
        currentDegree = 0f
        currentSmoothedDegree = 0f
        dataList = AverageList(10)
        for (l in listeners) {
            l.onCompassStopped()
        }
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == sensor) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
        } else if (event.sensor == magnetSensor) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
        }
        if (lastAccelerometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthInOrientation = orientation[0]
            var azimuthInDegrees = Math.toDegrees(azimuthInOrientation.toDouble()).toFloat()
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360f
            }
            convertBearingToTextAndUpdateView(azimuthInDegrees)
            currentDegree = cleanDegrees(azimuthInDegrees + declination)
            informListenersAboutNewDegree(currentDegree)
            currentSmoothedDegree = dataList.addAndGetAverage(currentDegree)
            informListenersAboutNewSmoothedDegree(currentSmoothedDegree)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    private fun convertBearingToTextAndUpdateView(bearing: Float) {
        val range = (bearing / (360f / 16f)).toInt()
        var dirTxt = ""
        if (range == 15 || range == 0) dirTxt = "N"
        if (range == 1 || range == 2) dirTxt = "NE"
        if (range == 3 || range == 4) dirTxt = "E"
        if (range == 5 || range == 6) dirTxt = "SE"
        if (range == 7 || range == 8) dirTxt = "S"
        if (range == 9 || range == 10) dirTxt = "SW"
        if (range == 11 || range == 12) dirTxt = "W"
        if (range == 13 || range == 14) dirTxt = "NW"
        for (l in listeners) {
            l.updateBearingText(
                "" + bearing.toInt() + 176.toChar() + " "
                        + dirTxt
            )
        }
    }

    private fun cleanDegrees(degree: Float): Float {
        val difference = Math.abs(currentDegree - degree)
        return if (difference > 180) {
            degree + if (currentDegree >= 0) 360 else -360
        } else {
            degree
        }
    }

    private fun informListenersAboutNewDegree(degree: Float) {
        for (l in listeners) {
            l.onNewDegreesToNorth(-degree)
        }
    }

    private fun informListenersAboutNewSmoothedDegree(degree: Float) {
        for (l in listeners) {
            l.onNewSmoothedDegreesToNorth(-degree)
        }
    }

    init {
        if (location != null) {
            val geomagneticField = GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(), location.altitude.toFloat(), Date().time
            )
            declination = geomagneticField.declination
        }
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }
}
package net.basicmodel.utils

import java.util.ArrayList

class AverageList(private val max: Int) : ArrayList<Float?>() {

    override fun add(element: Float?): Boolean {
        if (size >= max) {
            removeAt(0)
        }
        return super.add(element)
    }

    fun addAndGetAverage(data: Float?): Float {
        this.add(data)
        return average
    }

    private val average: Float
        get() {
            var sum = 0.0F
            for (data in this) {
                sum += data ?: 0F
            }
            return sum / size
        }
}
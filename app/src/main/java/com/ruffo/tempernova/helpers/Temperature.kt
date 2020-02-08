package com.ruffo.tempernova.helpers

import com.ruffo.tempernova.MainActivity
import com.ruffo.tempernova.R
import java.util.*

class Temperature {
    private var avgTemp = -1
    private var pastTemp = mutableListOf<Int>()

    private val numToAverage = 5
    private val thresholdTemp = 80
    private val coolThresholdTemp = 60

    private var averageRefills = mutableListOf<RefillData>()

    class RefillData(nRefills: Int, date: Date) {
        val nRefills: Int = nRefills
        val date: Date = date
    }

    fun updateAvgTemp(temp: Int) {
        if (temp > -100) {  // sensor gives < -100 values as error codes, so ignore it if we get one!
            checkIfRefilled(temp) // check if the user refilled the mug

            pastTemp.add(temp)
            if (pastTemp.size > numToAverage)
                pastTemp = pastTemp.drop(1).toMutableList()    // drop the first element!

            avgTemp = pastTemp.sum() / numToAverage
        }
    }

    fun checkIfRefilled(temp: Int) {
        if (temp > thresholdTemp && avgTemp < coolThresholdTemp) {
            val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity

            if (context !== null) {
                context.addRefil()
                context.updateRefillsCard(context.findViewById(R.id.nav_host_fragment))
            }
        }
    }

    fun getAverageRefills(): Float {
        if (averageRefills.isEmpty()) {
            return (MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity).nRefills.toFloat()
        }

        var nRefills = 0
        var nDays = averageRefills.size

        averageRefills.forEach { nRefills += it.nRefills }
        return (nRefills.toFloat() / nDays)
    }

    fun resetRefills() {

    }
}
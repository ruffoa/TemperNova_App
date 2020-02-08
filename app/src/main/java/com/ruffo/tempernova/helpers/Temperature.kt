package com.ruffo.tempernova.helpers

import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruffo.tempernova.MainActivity
import com.ruffo.tempernova.R
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Temperature {
    private var avgTemp = -1
    private var pastTemp = mutableListOf<Int>()
    private var wasCool = AtomicBoolean(true)
    private val gson = Gson()
    private var nRefills = 0

    private val numToAverage = 5
    private val thresholdTemp = 80
    private val coolThresholdTemp = 60

    private var averageRefills = mutableListOf<RefillData>()

    class RefillData(nRefills: Int, date: Date) {
        var nRefills: Int = nRefills
        val date: Date = date
    }

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

    fun getTimeOFDaysBefore(numBefore: Int): Long {
        return Date().time - (24 * 60 * 60 * 1000 * numBefore).toLong()
    }

    fun updateAvgTemp(temp: Int?) {
        if (temp !== null && temp > -100) {  // sensor gives < -100 values as error codes, so ignore it if we get one!
            checkIfRefilled(temp) // check if the user refilled the mug

            pastTemp.add(temp)
            if (pastTemp.size > numToAverage)
                pastTemp = pastTemp.drop(1).toMutableList()    // drop the first element!

            avgTemp =  temp

            if (pastTemp.isNotEmpty()) {
                avgTemp = pastTemp.sum() / numToAverage
            }

            if (avgTemp < coolThresholdTemp && temp < coolThresholdTemp) {
                wasCool.set(true)
            }

        }
    }

    fun addRefil() {
        nRefills++

        if (averageRefills.isEmpty()) {
            averageRefills.add(RefillData(nRefills, Date()))
            return
        }

        if (averageRefills.last().date.date == Date().date) {
            averageRefills.last().nRefills = nRefills
        } else {
            averageRefills.add(RefillData(nRefills, Date()))
        }
    }

    private fun checkIfRefilled(temp: Int) {
        if (temp > thresholdTemp && (avgTemp >= coolThresholdTemp && wasCool.get())) {
            wasCool.set(false)

            addRefil()

            val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?

            if (context !== null) {
                context.updateRefillsCard(context.findViewById(R.id.nav_host_fragment))
            }
        }
    }

    fun getAverageRefills(): Float {
        if (averageRefills.isEmpty()) {
            return nRefills.toFloat()
        }

        var nRefills = 0
        var nDays = averageRefills.size

        averageRefills.forEach { nRefills += it.nRefills }
        return (nRefills.toFloat() / nDays)
    }

    fun resetRefills() {
        averageRefills = mutableListOf()
    }

    fun getRefills(): MutableList<RefillData> {
        return averageRefills
    }

    fun getStringRefills(): String {
        return gson.toJson(averageRefills)
    }

    fun updateRefillsFromPrefs() {
        val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?

        if (context !== null) {
            val refillsString = context.readStringSharedPrefs("", context.getString(R.string.refills_preference_key))
            val temp: MutableList<RefillData> = gson.fromJson(refillsString)

            averageRefills =  temp
            val lastRefills = getTodaysRefills()

//            averageRefills = mutableListOf<RefillData>(RefillData(20, Date(getTimeOFDaysBefore(3))), RefillData(5, Date(getTimeOFDaysBefore(2))), RefillData(60, Date(getTimeOFDaysBefore(1))))
//            averageRefills.add(temp[0])

            nRefills = if (lastRefills !== null)
                lastRefills
            else
                0
        }

    }

    fun saveRefillsToPrefs() {
        val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?

        if (context !== null) {
            context.saveStringPref(getStringRefills(), context.getString(R.string.refills_preference_key))
        }
    }

    fun getTodaysRefills(): Int? {
        if (averageRefills.isEmpty()) {
            averageRefills.add(RefillData(nRefills, Date()))
        }

        if (averageRefills.last().date.date == Date().date)
            return averageRefills.last().nRefills

        averageRefills.add(RefillData(nRefills, Date()))
        return nRefills
    }
}
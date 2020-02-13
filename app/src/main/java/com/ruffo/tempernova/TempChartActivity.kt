package com.ruffo.tempernova

import android.content.Context
import android.graphics.Color
import android.graphics.Color.rgb
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.activity_chart.*
import kotlin.collections.ArrayList

class TempChartActivity : AppCompatActivity() {

    private var chart: LineChart? = null
    private lateinit var tempData: ArrayList<Entry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_chart)
        setSupportActionBar(toolbar)
        title = "Temperature"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val chart: LineChart = findViewById(R.id.tempChart)

        val data = getData()

        if (data === null)
            return

        setupChart(chart, data, colors[0])
        Handler().postDelayed({ getNewDataPoint(chart) }, 1000)    // wait for the next data point...
    }

    private val colors = arrayOf(
        rgb(137, 230, 81),
        rgb(240, 240, 30),
        rgb(89, 199, 250),
        rgb(250, 104, 104)
    );

    private fun setupChart(chart: LineChart, data: LineDataSet, color: Int) {

        // set custom chart offsets (automatic offset calculation is hereby disabled)
//        chart.setViewPortOffsets(10f, 0f, 10f, 0f)

        val lineData = LineData(data)

        // add data
        chart.data = lineData
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = TempValueFormatter()
        xAxis.granularity = 1f
        chart.axisLeft.granularity = 1f
        chart.axisRight.granularity = 1f

        // animate calls invalidate()...
        chart.animateX(2500)
    }

    fun Context.themeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute (attrRes, typedValue, true)
        return typedValue.data
    }

    class TempValueFormatter() : ValueFormatter() {

        override fun getFormattedValue(value: Float): String {
            return value.toString()
        }

//        override fun getAxisLabel(value: Float, axis: AxisBase): String {
//            val df: DateFormat = SimpleDateFormat("MMM dd")
//
//            Log.d("VLUE FORMATTER", "Calling the formatter... " + dateStr)
//            return dateStr
//        }
    }

    private fun getData(): LineDataSet? {

        val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?

        if (context === null)
            return null

        val currTemp = context.currTemp
        var set1 = LineDataSet(ArrayList<Entry>(), "Temperature")

        if (currTemp !== null) {
            tempData = ArrayList<Entry>()
            tempData.add(Entry(0f, currTemp.toFloat()))
            set1 = LineDataSet(tempData, "Temperature")
        }

        // create a dataset and give it a type
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        set1.color = themeColor(R.attr.colorPrimary);
        set1.highLightColor = themeColor(R.attr.colorAccent);
        set1.setDrawValues(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        set1.setLineWidth(2.5f);
        set1.setDrawCircles(false)

//        set1.setCircleRadius(4.5f);
        set1.setDrawFilled(true)
//        set1.setFillColor(ContextCompat.getColor(contex,R.color.pale_green));
//        s.setColor(ContextCompat.getColor(contex,R.color.pale_green));
        set1.fillColor = Color.GREEN

        set1.valueFormatter = TempValueFormatter()

        // create a data object with the data sets
        return set1
    }

    private fun getNewDataPoint(chart: LineChart) {
        val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?
        if (context === null)
            return

        val currTemp = context.currTemp
        if (currTemp === null) {
            return
        }

        if (chart !== null) {
            var data: LineData = (chart as LineChart).data

            Log.d("GETNEWDATAPOINT", "GEtting a new data point: " + data.entryCount)

            data.addEntry(Entry(data.entryCount.toFloat(), currTemp.toFloat()), data.dataSetCount - 1)

            data.notifyDataChanged()

            // let the chart know it's data has changed
            (chart).notifyDataSetChanged()
            (chart).invalidate()
        }

        Handler().postDelayed({ getNewDataPoint(chart) }, 1000)    // wait for the next data point...
    }

}



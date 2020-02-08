package com.ruffo.tempernova

import android.content.Context
import android.graphics.Color.rgb
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.ruffo.tempernova.helpers.Temperature
import kotlinx.android.synthetic.main.activity_chart.*
import java.text.DateFormat
import java.text.SimpleDateFormat


class ChartActivity : AppCompatActivity() {

    private var chart: BarChart? = null
    private var seekBarX: SeekBar? = null
    private var tvX: TextView? = null
    private lateinit var refillDataSet: MutableList<Temperature.RefillData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)
        setSupportActionBar(toolbar)

//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val chart: BarChart = findViewById(R.id.refillsChart)

        val data = getData()

        if (data === null)
            return

        setupChart(chart, data, colors[0])
    }

    private val colors = arrayOf(
        rgb(137, 230, 81),
        rgb(240, 240, 30),
        rgb(89, 199, 250),
        rgb(250, 104, 104)
    );

    private fun setupChart(chart: BarChart, data: BarDataSet, color: Int) {

        chart.setFitBars(true); // make the x-axis fit exactly all bars

        // set custom chart offsets (automatic offset calculation is hereby disabled)
//        chart.setViewPortOffsets(10f, 0f, 10f, 0f)

        val lineData = BarData(data)
        lineData.barWidth = 0.9f; // set custom bar width

        // add data
        chart.data = lineData;
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM;
        xAxis.setDrawGridLines(false);
        xAxis.valueFormatter = RefillValueFormatter(refillDataSet)
        xAxis.granularity = 1f;
        chart.axisLeft.granularity = 1f
        chart.axisRight.granularity = 1f

        // animate calls invalidate()...
        chart.animateX(2500);
    }

    fun Context.themeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute (attrRes, typedValue, true)
        return typedValue.data
    }

    class RefillValueFormatter(private val data: MutableList<Temperature.RefillData>) : ValueFormatter() {

        override fun getFormattedValue(value: Float): String {
            return value.toString()
        }

        override fun getBarLabel(barEntry: BarEntry?): String {
            val temp = data[barEntry!!.x.toInt()]
            Log.d("VLUE FORMATTER", "Calling the bar formatter...")
            return temp.date.toString()
//            return super.getBarLabel(barEntry]
        }

        override fun getAxisLabel(value: Float, axis: AxisBase): String {
            val temp = data[value.toInt()]

            val df: DateFormat = SimpleDateFormat("MMM dd")
            val dateStr: String = df.format(temp.date)

            Log.d("VLUE FORMATTER", "Calling the formatter... " + dateStr)
            return dateStr
        }
    }

    private fun getData(): BarDataSet? {

        val context = MainActivity.CoreHelper.contextGetter?.invoke() as MainActivity?

        if (context === null)
            return null

        val data = context.temperatureClass.getRefills()
        refillDataSet = data

        var values = ArrayList<BarEntry>()

        var i = 0

        data.forEach {
            values.add(BarEntry(i.toFloat(), it.nRefills.toFloat()))
            i++
        }

        // create a dataset and give it a type
        val set1: BarDataSet = BarDataSet(values, "Refills Per Day")
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        set1.color = themeColor(R.attr.colorPrimary);
        set1.highLightColor = themeColor(R.attr.colorAccent);
        set1.setDrawValues(false);


        set1.valueFormatter = RefillValueFormatter(data)

        // create a data object with the data sets
        return set1
    }

}



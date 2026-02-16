package com.hanyajasa.aplikasigiatramadhan

import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var spinnerDay: Spinner
    private lateinit var checkSubuh: CheckBox
    private lateinit var checkDzuhur: CheckBox
    private lateinit var checkAshar: CheckBox
    private lateinit var checkMaghrib: CheckBox
    private lateinit var checkIsya: CheckBox
    private lateinit var groupPuasa: RadioGroup
    private lateinit var radioPuasa: RadioButton
    private lateinit var radioTidakPuasa: RadioButton
    private lateinit var textSummary: TextView
    private lateinit var textScore: TextView
    private lateinit var textAchievement: TextView
    private lateinit var textStreak: TextView
    private lateinit var textImsakTime: TextView
    private lateinit var textSubuhTime: TextView
    private lateinit var textZuhurTime: TextView
    private lateinit var textAsarTime: TextView
    private lateinit var textMagribTime: TextView
    private lateinit var textIsyaTime: TextView
    private lateinit var editCatatan: TextInputEditText
    private lateinit var lineChartProgress: LineChart
    private lateinit var toneGenerator: ToneGenerator

    private var selectedDay = 1
    private var isUpdatingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences("giat_ramadhan", MODE_PRIVATE)
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

        bindViews()
        setupChart()
        setupDaySpinner()
        setupListeners()
        loadDayData(selectedDay)
    }

    private fun bindViews() {
        spinnerDay = findViewById(R.id.spinnerDay)
        checkSubuh = findViewById(R.id.checkSubuh)
        checkDzuhur = findViewById(R.id.checkDzuhur)
        checkAshar = findViewById(R.id.checkAshar)
        checkMaghrib = findViewById(R.id.checkMaghrib)
        checkIsya = findViewById(R.id.checkIsya)
        groupPuasa = findViewById(R.id.groupPuasa)
        radioPuasa = findViewById(R.id.radioPuasa)
        radioTidakPuasa = findViewById(R.id.radioTidakPuasa)
        textSummary = findViewById(R.id.textSummary)
        textScore = findViewById(R.id.textScore)
        textAchievement = findViewById(R.id.textAchievement)
        textStreak = findViewById(R.id.textStreak)
        textImsakTime = findViewById(R.id.textImsakTime)
        textSubuhTime = findViewById(R.id.textSubuhTime)
        textZuhurTime = findViewById(R.id.textZuhurTime)
        textAsarTime = findViewById(R.id.textAsarTime)
        textMagribTime = findViewById(R.id.textMagribTime)
        textIsyaTime = findViewById(R.id.textIsyaTime)
        editCatatan = findViewById(R.id.editCatatan)
        lineChartProgress = findViewById(R.id.lineChartProgress)
    }

    private fun setupChart() {
        lineChartProgress.description.isEnabled = false
        lineChartProgress.setTouchEnabled(true)
        lineChartProgress.setPinchZoom(true)
        lineChartProgress.legend.isEnabled = true

        lineChartProgress.axisRight.isEnabled = false
        lineChartProgress.axisLeft.axisMinimum = 0f
        lineChartProgress.axisLeft.axisMaximum = 100f
        lineChartProgress.axisLeft.granularity = 20f
        lineChartProgress.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)

        lineChartProgress.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChartProgress.xAxis.axisMinimum = 1f
        lineChartProgress.xAxis.axisMaximum = 30f
        lineChartProgress.xAxis.granularity = 1f
        lineChartProgress.xAxis.setLabelCount(10, false)
    }

    private fun setupDaySpinner() {
        val startDate = LocalDate.of(2026, 2, 18)
        val localeId = Locale.forLanguageTag("id-ID")
        val days = (1..30).map { day ->
            val gregorianDate = startDate.plusDays((day - 1).toLong())
            val dayName = gregorianDate.dayOfWeek.getDisplayName(TextStyle.FULL, localeId)
            val monthName = gregorianDate.month.getDisplayName(TextStyle.FULL, localeId)
            val prettyDayName = dayName.replaceFirstChar { it.titlecase(localeId) }
            val prettyMonthName = monthName.replaceFirstChar { it.titlecase(localeId) }
            "$day Ramadhan - $prettyDayName, ${gregorianDate.dayOfMonth} $prettyMonthName ${gregorianDate.year}"
        }
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        spinnerDay.setSelection(0)
        spinnerDay.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedDay = position + 1
                loadDayData(selectedDay)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No-op
            }
        }
    }

    private fun setupListeners() {
        val sholatChecks = listOf(checkSubuh, checkDzuhur, checkAshar, checkMaghrib, checkIsya)

        sholatChecks.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                if (isUpdatingUi) return@setOnCheckedChangeListener
                val previousBadgeTier = getBadgeTierByStreak(getPerfectStreakUntil(selectedDay))
                saveDayData()
                updateSummary(previousBadgeTier)
            }
        }

        groupPuasa.setOnCheckedChangeListener { _, _ ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            val previousBadgeTier = getBadgeTierByStreak(getPerfectStreakUntil(selectedDay))
            saveDayData()
            updateSummary(previousBadgeTier)
        }

        editCatatan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingUi) return
                saveDayData()
            }
        })
    }

    private fun loadDayData(day: Int) {
        isUpdatingUi = true

        checkSubuh.isChecked = prefs.getBoolean(key(day, "subuh"), false)
        checkDzuhur.isChecked = prefs.getBoolean(key(day, "dzuhur"), false)
        checkAshar.isChecked = prefs.getBoolean(key(day, "ashar"), false)
        checkMaghrib.isChecked = prefs.getBoolean(key(day, "maghrib"), false)
        checkIsya.isChecked = prefs.getBoolean(key(day, "isya"), false)

        when (prefs.getString(key(day, "puasa"), "")) {
            "puasa" -> groupPuasa.check(radioPuasa.id)
            "tidak" -> groupPuasa.check(radioTidakPuasa.id)
            else -> groupPuasa.clearCheck()
        }
        editCatatan.setText(prefs.getString(key(day, "catatan"), ""))
        updatePrayerTimes(day)

        isUpdatingUi = false
        updateSummary()
    }

    private fun saveDayData() {
        val puasaValue = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> "puasa"
            radioTidakPuasa.id -> "tidak"
            else -> ""
        }

        prefs.edit()
            .putBoolean(key(selectedDay, "subuh"), checkSubuh.isChecked)
            .putBoolean(key(selectedDay, "dzuhur"), checkDzuhur.isChecked)
            .putBoolean(key(selectedDay, "ashar"), checkAshar.isChecked)
            .putBoolean(key(selectedDay, "maghrib"), checkMaghrib.isChecked)
            .putBoolean(key(selectedDay, "isya"), checkIsya.isChecked)
            .putString(key(selectedDay, "puasa"), puasaValue)
            .putString(key(selectedDay, "catatan"), editCatatan.text?.toString()?.trim().orEmpty())
            .apply()
    }

    private fun updateSummary(previousBadgeTier: Int? = null) {
        val sholatDone = listOf(
            checkSubuh.isChecked,
            checkDzuhur.isChecked,
            checkAshar.isChecked,
            checkMaghrib.isChecked,
            checkIsya.isChecked
        ).count { it }

        val puasaDone = groupPuasa.checkedRadioButtonId == radioPuasa.id

        val puasaText = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> getString(R.string.status_puasa)
            radioTidakPuasa.id -> getString(R.string.status_tidak_puasa)
            else -> getString(R.string.status_belum)
        }

        val completedItems = sholatDone + if (puasaDone) 1 else 0
        val score = (completedItems * 100) / 6
        val streak = getPerfectStreakUntil(selectedDay)
        val currentBadgeTier = getBadgeTierByStreak(streak)
        val badge = getBadgeByStreak(streak)

        textSummary.text = getString(R.string.summary_format, sholatDone, puasaText)
        textScore.text = getString(R.string.score_format, score)
        textAchievement.text = getString(R.string.achievement_format, badge)
        textStreak.text = getString(R.string.streak_format, streak)

        if (previousBadgeTier != null && currentBadgeTier > previousBadgeTier) {
            animateBadgeUpgrade()
        }

        updateMonthlyChart()
    }

    private fun getPerfectStreakUntil(day: Int): Int {
        var streak = 0
        for (i in day downTo 1) {
            if (isPerfectDay(i)) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun isPerfectDay(day: Int): Boolean {
        val allSholatDone = listOf(
            prefs.getBoolean(key(day, "subuh"), false),
            prefs.getBoolean(key(day, "dzuhur"), false),
            prefs.getBoolean(key(day, "ashar"), false),
            prefs.getBoolean(key(day, "maghrib"), false),
            prefs.getBoolean(key(day, "isya"), false)
        ).all { it }

        val puasaDone = prefs.getString(key(day, "puasa"), "") == "puasa"
        return allSholatDone && puasaDone
    }

    private fun getBadgeByStreak(streak: Int): String {
        return when {
            streak >= 30 -> getString(R.string.badge_juara)
            streak >= 14 -> getString(R.string.badge_hebat)
            streak >= 7 -> getString(R.string.badge_terlatih)
            streak >= 3 -> getString(R.string.badge_pemula)
            else -> getString(R.string.badge_belum)
        }
    }

    private fun getBadgeTierByStreak(streak: Int): Int {
        return when {
            streak >= 30 -> 4
            streak >= 14 -> 3
            streak >= 7 -> 2
            streak >= 3 -> 1
            else -> 0
        }
    }

    private fun animateBadgeUpgrade() {
        val scaleX = ObjectAnimator.ofFloat(textAchievement, "scaleX", 1f, 1.08f, 1f)
        val scaleY = ObjectAnimator.ofFloat(textAchievement, "scaleY", 1f, 1.08f, 1f)
        val alpha = ObjectAnimator.ofFloat(textAchievement, "alpha", 0.7f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 320
            start()
        }

        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 140)
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }

    private fun updateMonthlyChart() {
        val sholatEntries = mutableListOf<Entry>()
        val puasaEntries = mutableListOf<Entry>()

        for (day in 1..30) {
            val sholatCount = listOf(
                prefs.getBoolean(key(day, "subuh"), false),
                prefs.getBoolean(key(day, "dzuhur"), false),
                prefs.getBoolean(key(day, "ashar"), false),
                prefs.getBoolean(key(day, "maghrib"), false),
                prefs.getBoolean(key(day, "isya"), false)
            ).count { it }

            val sholatScore = (sholatCount * 100f) / 5f
            val puasaScore = if (prefs.getString(key(day, "puasa"), "") == "puasa") 100f else 0f

            sholatEntries.add(Entry(day.toFloat(), sholatScore))
            puasaEntries.add(Entry(day.toFloat(), puasaScore))
        }

        val sholatSet = LineDataSet(sholatEntries, getString(R.string.legend_sholat)).apply {
            color = Color.parseColor("#2E7D32")
            setCircleColor(Color.parseColor("#2E7D32"))
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val puasaSet = LineDataSet(puasaEntries, getString(R.string.legend_puasa)).apply {
            color = Color.parseColor("#EF6C00")
            setCircleColor(Color.parseColor("#EF6C00"))
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        lineChartProgress.data = LineData(sholatSet, puasaSet)
        lineChartProgress.invalidate()
    }

    private fun updatePrayerTimes(day: Int) {
        val times = prayerTimesByDay.getOrNull(day - 1) ?: return
        textImsakTime.text = getString(R.string.waktu_template, getString(R.string.label_imsak), times.imsak)
        textSubuhTime.text = getString(R.string.waktu_template, getString(R.string.label_subuh_waktu), times.subuh)
        textZuhurTime.text = getString(R.string.waktu_template, getString(R.string.label_zuhur_waktu), times.zuhur)
        textAsarTime.text = getString(R.string.waktu_template, getString(R.string.label_asar_waktu), times.asar)
        textMagribTime.text = getString(R.string.waktu_template, getString(R.string.label_magrib_waktu), times.magrib)
        textIsyaTime.text = getString(R.string.waktu_template, getString(R.string.label_isya_waktu), times.isya)
    }

    private fun key(day: Int, field: String): String = "day_${day}_$field"

    private data class PrayerTimes(
        val imsak: String,
        val subuh: String,
        val zuhur: String,
        val asar: String,
        val magrib: String,
        val isya: String
    )

    private val prayerTimesByDay = listOf(
        PrayerTimes("04:07", "04:17", "11:42", "14:57", "17:47", "18:56"),
        PrayerTimes("04:07", "04:17", "11:42", "14:56", "17:47", "18:56"),
        PrayerTimes("04:07", "04:17", "11:42", "14:55", "17:46", "18:56"),
        PrayerTimes("04:07", "04:17", "11:42", "14:55", "17:46", "18:56"),
        PrayerTimes("04:07", "04:17", "11:41", "14:54", "17:46", "18:55"),
        PrayerTimes("04:07", "04:17", "11:41", "14:54", "17:46", "18:55"),
        PrayerTimes("04:07", "04:17", "11:41", "14:53", "17:46", "18:55"),
        PrayerTimes("04:07", "04:17", "11:41", "14:52", "17:45", "18:54"),
        PrayerTimes("04:07", "04:17", "11:41", "14:52", "17:45", "18:54"),
        PrayerTimes("04:07", "04:17", "11:41", "14:51", "17:45", "18:54"),
        PrayerTimes("04:07", "04:17", "11:40", "14:50", "17:45", "18:53"),
        PrayerTimes("04:07", "04:17", "11:40", "14:49", "17:44", "18:53"),
        PrayerTimes("04:07", "04:17", "11:40", "14:48", "17:44", "18:53"),
        PrayerTimes("04:07", "04:17", "11:40", "14:48", "17:44", "18:52"),
        PrayerTimes("04:07", "04:17", "11:40", "14:47", "17:44", "18:52"),
        PrayerTimes("04:07", "04:17", "11:39", "14:46", "17:43", "18:52"),
        PrayerTimes("04:07", "04:17", "11:39", "14:45", "17:43", "18:51"),
        PrayerTimes("04:07", "04:17", "11:39", "14:44", "17:43", "18:51"),
        PrayerTimes("04:07", "04:17", "11:39", "14:43", "17:42", "18:51"),
        PrayerTimes("04:06", "04:16", "11:38", "14:42", "17:42", "18:50"),
        PrayerTimes("04:06", "04:16", "11:38", "14:41", "17:42", "18:50"),
        PrayerTimes("04:06", "04:16", "11:38", "14:40", "17:41", "18:50"),
        PrayerTimes("04:06", "04:16", "11:38", "14:39", "17:41", "18:49"),
        PrayerTimes("04:06", "04:16", "11:37", "14:38", "17:41", "18:49"),
        PrayerTimes("04:06", "04:16", "11:37", "14:37", "17:40", "18:49"),
        PrayerTimes("04:05", "04:15", "11:37", "14:36", "17:40", "18:48"),
        PrayerTimes("04:05", "04:15", "11:37", "14:36", "17:40", "18:48"),
        PrayerTimes("04:05", "04:15", "11:36", "14:37", "17:39", "18:48"),
        PrayerTimes("04:05", "04:15", "11:36", "14:37", "17:39", "18:47"),
        PrayerTimes("04:05", "04:15", "11:36", "14:38", "17:39", "18:47")
    )
}

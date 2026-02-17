package com.hanyajasa.aplikasigiatramadhan

import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import android.text.TextWatcher
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.net.HttpURLConnection
import kotlin.math.abs
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.net.URLEncoder
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var spinnerDay: Spinner
    private lateinit var checkSubuh: CheckBox
    private lateinit var checkDzuhur: CheckBox
    private lateinit var checkAshar: CheckBox
    private lateinit var checkMaghrib: CheckBox
    private lateinit var checkIsya: CheckBox
    private lateinit var checkTarawih: CheckBox
    private lateinit var checkWitir: CheckBox
    private lateinit var checkTadarus: CheckBox
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
    private lateinit var textAppVersion: TextView
    private lateinit var textUpdateStatus: TextView
    private lateinit var buttonAbout: Button
    private lateinit var buttonMasukanSaran: Button
    private lateinit var buttonCheckUpdate: Button
    private lateinit var gridHeatmap: GridLayout
    private lateinit var editCatatan: TextInputEditText
    private lateinit var lineChartProgress: LineChart
    private lateinit var toneGenerator: ToneGenerator

    private var selectedDay = 1
    private var isUpdatingUi = false
    private var downloadId: Long = -1L
    private var receiverRegistered = false
    private var startupStatsScheduled = false
    private val heatmapCells = mutableListOf<TextView>()
    private val startupHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val CURRENT_VERSION = 20260217
        private const val VERSION_URL = "https://hanyajasa.com/apk/giatramadhan/versi.txt"
        private const val APK_URL = "https://hanyajasa.com/apk/giatramadhan/giatramadhan.apk"
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != downloadId) return

            textUpdateStatus.text = getString(R.string.update_status_downloaded)
            installDownloadedApk(id)
        }
    }

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
        loadPrayerTimesFromCsv()

        bindViews()
        setupUpdateUi()
        setupHeatmap()
        setupChart()
        setupDaySpinner()
        setupListeners()
        loadDayData(selectedDay)
        registerDownloadReceiver()
        scheduleStartupStats()
    }

    private fun bindViews() {
        spinnerDay = findViewById(R.id.spinnerDay)
        checkSubuh = findViewById(R.id.checkSubuh)
        checkDzuhur = findViewById(R.id.checkDzuhur)
        checkAshar = findViewById(R.id.checkAshar)
        checkMaghrib = findViewById(R.id.checkMaghrib)
        checkIsya = findViewById(R.id.checkIsya)
        checkTarawih = findViewById(R.id.checkTarawih)
        checkWitir = findViewById(R.id.checkWitir)
        checkTadarus = findViewById(R.id.checkTadarus)
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
        textAppVersion = findViewById(R.id.textAppVersion)
        textUpdateStatus = findViewById(R.id.textUpdateStatus)
        buttonAbout = findViewById(R.id.buttonAbout)
        buttonMasukanSaran = findViewById(R.id.buttonMasukanSaran)
        buttonCheckUpdate = findViewById(R.id.buttonCheckUpdate)
        gridHeatmap = findViewById(R.id.gridHeatmap)
        editCatatan = findViewById(R.id.editCatatan)
        lineChartProgress = findViewById(R.id.lineChartProgress)
    }

    private fun setupUpdateUi() {
        textAppVersion.text = getString(R.string.app_version_label, CURRENT_VERSION.toString())
        textUpdateStatus.text = getString(R.string.update_status_idle)
        buttonAbout.setOnClickListener {
            showAboutDialog()
        }
        buttonMasukanSaran.setOnClickListener {
            showMasukanSaranDialog()
        }
        buttonCheckUpdate.setOnClickListener {
            checkForUpdates()
        }
    }

    private fun showMasukanSaranDialog() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.masukan_dialog_hint)
            minLines = 3
            maxLines = 6
        }

        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val container = LinearLayout(this).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.masukan_dialog_title))
            .setView(container)
            .setNegativeButton(getString(R.string.masukan_batal), null)
            .setPositiveButton(getString(R.string.masukan_kirim)) { _, _ ->
                val message = input.text?.toString()?.trim().orEmpty()
                if (message.isBlank()) {
                    Toast.makeText(this, getString(R.string.masukan_kosong), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendMasukanSaran(message)
            }
            .show()
    }

    private fun sendMasukanSaran(message: String) {
        if (!isInternetActive()) {
            textUpdateStatus.text = getString(R.string.masukan_status_gagal)
            return
        }

        textUpdateStatus.text = getString(R.string.masukan_status_mengirim)
        Thread {
            runCatching {
                val encodedPayload = URLEncoder.encode(message, "UTF-8")
                val endpoint = "https://hanyajasa.com/?MasukanSaranGiatRamadhan=$encodedPayload"
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { it.readBytes() }
                connection.disconnect()
            }.onSuccess {
                runOnUiThread {
                    textUpdateStatus.text = getString(R.string.masukan_status_berhasil)
                }
            }.onFailure {
                runOnUiThread {
                    textUpdateStatus.text = getString(R.string.masukan_status_gagal)
                }
            }
        }.start()
    }

    private fun showAboutDialog() {
        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val marginTop = (12 * density).toInt()

        val container = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val aboutText = TextView(this).apply {
            text = getString(R.string.about_dialog_message, CURRENT_VERSION.toString())
            setTextColor(Color.parseColor("#1A3C34"))
            textSize = 14f
        }

        val donationTitle = TextView(this).apply {
            text = getString(R.string.donasi_title)
            setTextColor(Color.parseColor("#1A3C34"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = marginTop
            }
        }

        val qrisImage = ImageView(this).apply {
            setImageResource(R.drawable.qris_donasi)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = marginTop
            }
        }

        val donationAccounts = TextView(this).apply {
            text = getString(R.string.donasi_rekening)
            setTextColor(Color.parseColor("#2F5249"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = marginTop
            }
        }

        content.addView(aboutText)
        content.addView(donationTitle)
        content.addView(qrisImage)
        content.addView(donationAccounts)
        container.addView(content)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_dialog_title))
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun checkForUpdates() {
        textUpdateStatus.text = getString(R.string.update_status_checking)
        Thread {
            runCatching {
                URL(VERSION_URL).readText().trim()
            }.onSuccess { versionText ->
                val latest = versionText.toLongOrNull()
                runOnUiThread {
                    if (latest == null) {
                        textUpdateStatus.text = getString(R.string.update_status_failed)
                        return@runOnUiThread
                    }

                    if (latest > CURRENT_VERSION) {
                        textUpdateStatus.text = getString(R.string.update_status_available, latest.toString())
                        showUpdateDialog(latest.toString())
                    } else {
                        textUpdateStatus.text = getString(R.string.update_status_latest)
                    }
                }
            }.onFailure {
                runOnUiThread {
                    textUpdateStatus.text = getString(R.string.update_status_failed)
                }
            }
        }.start()
    }

    private fun showUpdateDialog(newVersion: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_dialog_title))
            .setMessage(getString(R.string.update_dialog_message, newVersion))
            .setPositiveButton(getString(R.string.update_yes)) { _, _ ->
                downloadLatestApk()
            }
            .setNegativeButton(getString(R.string.update_no), null)
            .show()
    }

    private fun downloadLatestApk() {
        val request = DownloadManager.Request(android.net.Uri.parse(APK_URL))
            .setTitle("Giat Ramadhan Update")
            .setDescription("Mengunduh aplikasi terbaru...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "giatramadhan.apk")
            .setMimeType("application/vnd.android.package-archive")

        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)
        textUpdateStatus.text = getString(R.string.update_status_downloading)
    }

    private fun installDownloadedApk(downloadedId: Long) {
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val apkUri = dm.getUriForDownloadedFile(downloadedId) ?: return

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(installIntent) }
    }

    private fun registerDownloadReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun setupChart() {
        lineChartProgress.description.isEnabled = false
        lineChartProgress.setTouchEnabled(true)
        lineChartProgress.setPinchZoom(false)
        lineChartProgress.setScaleEnabled(false)
        lineChartProgress.legend.isEnabled = true

        lineChartProgress.axisRight.isEnabled = false
        lineChartProgress.axisLeft.axisMinimum = 0f
        lineChartProgress.axisLeft.axisMaximum = 100f
        lineChartProgress.axisLeft.granularity = 20f
        lineChartProgress.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)

        lineChartProgress.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChartProgress.xAxis.axisMinimum = 0f
        lineChartProgress.xAxis.axisMaximum = 29f
        lineChartProgress.xAxis.granularity = 1f
        lineChartProgress.xAxis.isGranularityEnabled = true
        lineChartProgress.xAxis.setLabelCount(30, false)
        lineChartProgress.xAxis.textSize = 8f
        lineChartProgress.xAxis.labelRotationAngle = -60f
        lineChartProgress.xAxis.setAvoidFirstLastClipping(false)
        lineChartProgress.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val index = value.roundToInt()
                return if (index in 0..29 && abs(value - index.toFloat()) <= 0.51f) {
                    (index + 1).toString()
                } else {
                    ""
                }
            }
        }
    }

    private fun setupHeatmap() {
        gridHeatmap.removeAllViews()
        heatmapCells.clear()

        val density = resources.displayMetrics.density
        val cellSize = (44 * density).toInt()
        val margin = (4 * density).toInt()

        for (day in 1..30) {
            val cell = TextView(this).apply {
                text = day.toString()
                gravity = android.view.Gravity.CENTER
                textSize = 12f
                setTextColor(Color.parseColor("#1A3C34"))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(4, 4, 4, 4)
            }
            heatmapCells.add(cell)
            gridHeatmap.addView(cell)
        }
        updateHeatmap()
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
        val sholatChecks = listOf(
            checkSubuh,
            checkDzuhur,
            checkAshar,
            checkMaghrib,
            checkIsya,
            checkTarawih,
            checkWitir
        )
        val ibadahChecks = sholatChecks + checkTadarus

        ibadahChecks.forEach { checkBox ->
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
        checkTarawih.isChecked = prefs.getBoolean(key(day, "tarawih"), false)
        checkWitir.isChecked = prefs.getBoolean(key(day, "witir"), false)
        checkTadarus.isChecked = prefs.getBoolean(key(day, "tadarus"), false)

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
            .putBoolean(key(selectedDay, "tarawih"), checkTarawih.isChecked)
            .putBoolean(key(selectedDay, "witir"), checkWitir.isChecked)
            .putBoolean(key(selectedDay, "tadarus"), checkTadarus.isChecked)
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
            checkIsya.isChecked,
            checkTarawih.isChecked,
            checkWitir.isChecked
        ).count { it }

        val puasaDone = groupPuasa.checkedRadioButtonId == radioPuasa.id
        val tadarusDone = checkTadarus.isChecked

        val puasaText = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> getString(R.string.status_puasa)
            radioTidakPuasa.id -> getString(R.string.status_tidak_puasa)
            else -> getString(R.string.status_belum)
        }
        val tadarusText = if (tadarusDone) {
            getString(R.string.status_tadarus_done)
        } else {
            getString(R.string.status_tadarus_not_done)
        }

        val completedItems = sholatDone + (if (puasaDone) 1 else 0) + (if (tadarusDone) 1 else 0)
        val score = (completedItems * 100) / 9
        val streak = getPerfectStreakUntil(selectedDay)
        val currentBadgeTier = getBadgeTierByStreak(streak)
        val badge = getBadgeByStreak(streak)

        textSummary.text = getString(R.string.summary_format_with_tadarus, sholatDone, puasaText, tadarusText)
        textScore.text = getString(R.string.score_format, score)
        textAchievement.text = getString(R.string.achievement_format, badge)
        textStreak.text = getString(R.string.streak_format, streak)

        if (previousBadgeTier != null && currentBadgeTier > previousBadgeTier) {
            animateBadgeUpgrade()
        }

        updateMonthlyChart()
        updateHeatmap()
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
            prefs.getBoolean(key(day, "isya"), false),
            prefs.getBoolean(key(day, "tarawih"), false),
            prefs.getBoolean(key(day, "witir"), false)
        ).all { it }

        val puasaDone = prefs.getString(key(day, "puasa"), "") == "puasa"
        val tadarusDone = prefs.getBoolean(key(day, "tadarus"), false)
        return allSholatDone && puasaDone && tadarusDone
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
        startupHandler.removeCallbacksAndMessages(null)
        if (receiverRegistered) {
            unregisterReceiver(downloadReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
        toneGenerator.release()
    }

    private fun updateHeatmap() {
        if (heatmapCells.isEmpty()) return

        for (day in 1..30) {
            val cell = heatmapCells[day - 1]
            val score = calculateDayScore(day)
            val bgColor = when {
                score >= 100 -> Color.parseColor("#2E7D32")
                score >= 80 -> Color.parseColor("#66BB6A")
                score >= 60 -> Color.parseColor("#A5D6A7")
                score >= 40 -> Color.parseColor("#FFE082")
                score >= 20 -> Color.parseColor("#FFCC80")
                else -> Color.parseColor("#F5E9E2")
            }
            cell.setBackgroundColor(bgColor)
            cell.setTextColor(if (score >= 80) Color.WHITE else Color.parseColor("#1A3C34"))
            cell.contentDescription = "Hari $day skor $score"
        }
    }

    private fun calculateDayScore(day: Int): Int {
        val sholatDone = listOf(
            prefs.getBoolean(key(day, "subuh"), false),
            prefs.getBoolean(key(day, "dzuhur"), false),
            prefs.getBoolean(key(day, "ashar"), false),
            prefs.getBoolean(key(day, "maghrib"), false),
            prefs.getBoolean(key(day, "isya"), false),
            prefs.getBoolean(key(day, "tarawih"), false),
            prefs.getBoolean(key(day, "witir"), false)
        ).count { it }
        val puasaDone = prefs.getString(key(day, "puasa"), "") == "puasa"
        val tadarusDone = prefs.getBoolean(key(day, "tadarus"), false)
        val completedItems = sholatDone + (if (puasaDone) 1 else 0) + (if (tadarusDone) 1 else 0)
        return (completedItems * 100) / 9
    }

    private fun updateMonthlyChart() {
        val sholatEntries = mutableListOf<Entry>()
        val puasaEntries = mutableListOf<Entry>()
        val tadarusEntries = mutableListOf<Entry>()

        for (day in 1..30) {
            val sholatCount = listOf(
                prefs.getBoolean(key(day, "subuh"), false),
                prefs.getBoolean(key(day, "dzuhur"), false),
                prefs.getBoolean(key(day, "ashar"), false),
                prefs.getBoolean(key(day, "maghrib"), false),
                prefs.getBoolean(key(day, "isya"), false),
                prefs.getBoolean(key(day, "tarawih"), false),
                prefs.getBoolean(key(day, "witir"), false)
            ).count { it }

            val sholatScore = (sholatCount * 100f) / 7f
            val puasaScore = if (prefs.getString(key(day, "puasa"), "") == "puasa") 100f else 0f
            val tadarusScore = if (prefs.getBoolean(key(day, "tadarus"), false)) 100f else 0f

            val x = (day - 1).toFloat()
            sholatEntries.add(Entry(x, sholatScore))
            puasaEntries.add(Entry(x, puasaScore))
            tadarusEntries.add(Entry(x, tadarusScore))
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

        val tadarusSet = LineDataSet(tadarusEntries, getString(R.string.legend_tadarus)).apply {
            color = Color.parseColor("#1565C0")
            setCircleColor(Color.parseColor("#1565C0"))
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        lineChartProgress.data = LineData(sholatSet, puasaSet, tadarusSet)
        lineChartProgress.invalidate()
    }

    private fun updatePrayerTimes(day: Int) {
        val times = prayerTimesByDay.getOrNull(day - 1)
        if (times == null) {
            textImsakTime.text = getString(R.string.waktu_template, getString(R.string.label_imsak), "-")
            textSubuhTime.text = getString(R.string.waktu_template, getString(R.string.label_subuh_waktu), "-")
            textZuhurTime.text = getString(R.string.waktu_template, getString(R.string.label_zuhur_waktu), "-")
            textAsarTime.text = getString(R.string.waktu_template, getString(R.string.label_asar_waktu), "-")
            textMagribTime.text = getString(R.string.waktu_template, getString(R.string.label_magrib_waktu), "-")
            textIsyaTime.text = getString(R.string.waktu_template, getString(R.string.label_isya_waktu), "-")
            return
        }
        textImsakTime.text = getString(R.string.waktu_template, getString(R.string.label_imsak), times.imsak)
        textSubuhTime.text = getString(R.string.waktu_template, getString(R.string.label_subuh_waktu), times.subuh)
        textZuhurTime.text = getString(R.string.waktu_template, getString(R.string.label_zuhur_waktu), times.zuhur)
        textAsarTime.text = getString(R.string.waktu_template, getString(R.string.label_asar_waktu), times.asar)
        textMagribTime.text = getString(R.string.waktu_template, getString(R.string.label_magrib_waktu), times.magrib)
        textIsyaTime.text = getString(R.string.waktu_template, getString(R.string.label_isya_waktu), times.isya)
    }

    private fun loadPrayerTimesFromCsv() {
        val loaded = mutableListOf<PrayerTimes>()
        runCatching {
            assets.open("Ramadhan1447H.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    if (line.isBlank()) return@forEach
                    val fields = parseCsvLine(line)
                    if (fields.size < 10) return@forEach
                    loaded.add(
                        PrayerTimes(
                            imsak = fields[2].trim(),
                            subuh = fields[3].trim(),
                            zuhur = fields[6].trim(),
                            asar = fields[7].trim(),
                            magrib = fields[8].trim(),
                            isya = fields[9].trim()
                        )
                    )
                }
            }
        }
        prayerTimesByDay.clear()
        prayerTimesByDay.addAll(loaded)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        line.forEach { ch ->
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun key(day: Int, field: String): String = "day_${day}_$field"

    private fun scheduleStartupStats() {
        if (startupStatsScheduled) return
        startupStatsScheduled = true

        startupHandler.postDelayed({
            sendStartupStatsIfOnline()
        }, 5000L)
    }

    private fun sendStartupStatsIfOnline() {
        if (!isInternetActive()) return

        Thread {
            runCatching {
                val payload = buildStartupStatsPayload()
                val encodedPayload = URLEncoder.encode(payload, "UTF-8")
                val endpoint = "https://hanyajasa.com/?StatistikGiatRamadhan=$encodedPayload"
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { it.readBytes() }
                connection.disconnect()
            }
        }.start()
    }

    private fun isInternetActive(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildStartupStatsPayload(): String {
        val androidVersion = Build.VERSION.RELEASE ?: "unknown"
        return "appVersion=$CURRENT_VERSION;" +
            "brand=${Build.BRAND};" +
            "manufacturer=${Build.MANUFACTURER};" +
            "model=${Build.MODEL};" +
            "device=${Build.DEVICE};" +
            "product=${Build.PRODUCT};" +
            "sdk=${Build.VERSION.SDK_INT};" +
            "android=$androidVersion"
    }

    private data class PrayerTimes(
        val imsak: String,
        val subuh: String,
        val zuhur: String,
        val asar: String,
        val magrib: String,
        val isya: String
    )

    private val prayerTimesByDay = mutableListOf<PrayerTimes>()
}

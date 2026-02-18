package com.hanyajasa.aplikasigiatramadhan

import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Path
import android.text.Editable
import android.text.TextWatcher
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.FrameLayout
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.net.URL
import kotlin.math.max
import kotlin.random.Random

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
    private lateinit var groupAlasanTidakPuasa: RadioGroup
    private lateinit var radioPuasa: RadioButton
    private lateinit var radioTidakPuasa: RadioButton
    private lateinit var radioAlasanSakit: RadioButton
    private lateinit var radioAlasanSafar: RadioButton
    private lateinit var radioAlasanHaid: RadioButton
    private lateinit var textSummary: TextView
    private lateinit var textScore: TextView
    private lateinit var textAchievement: TextView
    private lateinit var textBadgeStars: TextView
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
    private lateinit var buttonShareEmblem: Button
    private lateinit var buttonCheckUpdate: Button
    private lateinit var buttonChangeCalendar: Button
    private lateinit var gridHeatmap: GridLayout
    private lateinit var badgeOverlay: FrameLayout
    private lateinit var imageBadgeIcon: ImageView
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
        private const val CURRENT_VERSION = 20260218
        private const val VERSION_URL = "https://hanyajasa.com/apk/giatramadhan/versi.txt"
        private const val APK_URL = "https://hanyajasa.com/apk/giatramadhan/giatramadhan.apk"
        private const val PREF_CALENDAR_MODE = "calendar_mode"
        private const val CALENDAR_MODE_KHGT = "khgt"
        private const val CALENDAR_MODE_KEMENAG = "kemenag"
        private const val CALENDAR_ASSET_KHGT = "Ramadhan1447H.csv"
        private const val CALENDAR_ASSET_KEMENAG = "Ramadhan1447H_Versi_Kemenag.csv"
        private val START_DATE_KHGT: LocalDate = LocalDate.of(2026, 2, 18)
        private val START_DATE_KEMENAG: LocalDate = LocalDate.of(2026, 2, 19)
    }

    private data class EmblemStats(
        val totalScore: Int,
        val bestStreak: Int,
        val badgeLabel: String
    )

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != downloadId) return

            handleDownloadCompleted(id)
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

        bindViews()
        setupUpdateUi()
        setupHeatmap()
        setupChart()
        setupListeners()
        registerDownloadReceiver()
        ensureCalendarSelectionAndLoadData()
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
        groupAlasanTidakPuasa = findViewById(R.id.groupAlasanTidakPuasa)
        radioPuasa = findViewById(R.id.radioPuasa)
        radioTidakPuasa = findViewById(R.id.radioTidakPuasa)
        radioAlasanSakit = findViewById(R.id.radioAlasanSakit)
        radioAlasanSafar = findViewById(R.id.radioAlasanSafar)
        radioAlasanHaid = findViewById(R.id.radioAlasanHaid)
        textSummary = findViewById(R.id.textSummary)
        textScore = findViewById(R.id.textScore)
        textAchievement = findViewById(R.id.textAchievement)
        textBadgeStars = findViewById(R.id.textBadgeStars)
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
        buttonShareEmblem = findViewById(R.id.buttonShareEmblem)
        buttonCheckUpdate = findViewById(R.id.buttonCheckUpdate)
        buttonChangeCalendar = findViewById(R.id.buttonChangeCalendar)
        gridHeatmap = findViewById(R.id.gridHeatmap)
        badgeOverlay = findViewById(R.id.badgeOverlay)
        imageBadgeIcon = findViewById(R.id.imageBadgeIcon)
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
        buttonShareEmblem.setOnClickListener {
            shareAchievementEmblem()
        }
        buttonCheckUpdate.setOnClickListener {
            checkForUpdates()
        }
        buttonChangeCalendar.setOnClickListener {
            showCalendarSelectionDialog(forceChoice = false)
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
                val connection = URL(VERSION_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                val response = connection.inputStream.bufferedReader().use { it.readText().trim() }
                connection.disconnect()
                response
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
        val apkUri = dm.getUriForDownloadedFile(downloadedId)
        if (apkUri == null) {
            textUpdateStatus.text = getString(R.string.update_status_install_failed)
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(installIntent) }
            .onFailure {
                textUpdateStatus.text = getString(R.string.update_status_install_failed)
            }
    }

    private fun handleDownloadCompleted(downloadedId: Long) {
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadedId)
        val cursor = dm.query(query) ?: run {
            textUpdateStatus.text = getString(R.string.update_status_download_failed)
            return
        }
        cursor.use {
            if (!it.moveToFirst()) {
                textUpdateStatus.text = getString(R.string.update_status_download_failed)
                return
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                textUpdateStatus.text = getString(R.string.update_status_downloaded)
                installDownloadedApk(downloadedId)
                return
            }
            textUpdateStatus.text = getString(R.string.update_status_download_failed)
        }
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

    private fun ensureCalendarSelectionAndLoadData() {
        val savedMode = prefs.getString(PREF_CALENDAR_MODE, null)
        if (savedMode == null) {
            showCalendarSelectionDialog(forceChoice = true)
            return
        }
        loadPrayerTimesFromCsv(getCalendarAssetByMode(savedMode))
        setupDaySpinner(savedMode)
        loadDayData(selectedDay)
    }

    private fun showCalendarSelectionDialog(forceChoice: Boolean) {
        val currentMode = prefs.getString(PREF_CALENDAR_MODE, CALENDAR_MODE_KHGT)
        val options = arrayOf(
            getString(R.string.calendar_option_khgt),
            getString(R.string.calendar_option_kemenag)
        )
        var selectedIndex = if (currentMode == CALENDAR_MODE_KEMENAG) 1 else 0

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.calendar_dialog_title))
            .setCancelable(!forceChoice)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(getString(R.string.calendar_dialog_use)) { _, _ ->
                val mode = if (selectedIndex == 0) CALENDAR_MODE_KHGT else CALENDAR_MODE_KEMENAG
                prefs.edit().putString(PREF_CALENDAR_MODE, mode).apply()
                loadPrayerTimesFromCsv(getCalendarAssetByMode(mode))
                setupDaySpinner(mode)
                loadDayData(selectedDay)
            }
            .apply {
                if (!forceChoice) {
                    setNegativeButton(android.R.string.cancel, null)
                }
            }
            .create()

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun getCalendarAssetByMode(mode: String): String {
        return if (mode == CALENDAR_MODE_KEMENAG) CALENDAR_ASSET_KEMENAG else CALENDAR_ASSET_KHGT
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

    private fun setupDaySpinner(calendarMode: String) {
        val startDate = if (calendarMode == CALENDAR_MODE_KEMENAG) START_DATE_KEMENAG else START_DATE_KHGT
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
        val safeDay = selectedDay.coerceIn(1, 30)
        spinnerDay.setSelection(safeDay - 1)
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
            updateAlasanTidakPuasaVisibility()
            val previousBadgeTier = getBadgeTierByStreak(getPerfectStreakUntil(selectedDay))
            saveDayData()
            updateSummary(previousBadgeTier)
        }

        groupAlasanTidakPuasa.setOnCheckedChangeListener { _, _ ->
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
        when (prefs.getString(key(day, "alasan_tidak_puasa"), "")) {
            "sakit" -> groupAlasanTidakPuasa.check(radioAlasanSakit.id)
            "safar" -> groupAlasanTidakPuasa.check(radioAlasanSafar.id)
            "haid" -> groupAlasanTidakPuasa.check(radioAlasanHaid.id)
            else -> groupAlasanTidakPuasa.clearCheck()
        }
        updateAlasanTidakPuasaVisibility()
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
        val alasanTidakPuasa = if (puasaValue == "tidak") getSelectedAlasanTidakPuasa() else ""

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
            .putString(key(selectedDay, "alasan_tidak_puasa"), alasanTidakPuasa)
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

        val puasaDone = isPuasaCountedForScoreForCurrentDay()
        val tadarusDone = checkTadarus.isChecked

        val puasaText = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> getString(R.string.status_puasa)
            radioTidakPuasa.id -> {
                val alasan = getSelectedAlasanTidakPuasaLabel()
                if (alasan.isBlank()) getString(R.string.status_tidak_puasa)
                else getString(R.string.status_tidak_puasa_dengan_alasan, alasan)
            }
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
        val isTierUpgrade = previousBadgeTier != null && currentBadgeTier > previousBadgeTier
        updateBadgeVisual(currentBadgeTier, score, isTierUpgrade)
        textStreak.text = getString(R.string.streak_format, streak)
        maybeShowHighScoreOverlay(score)

        if (isTierUpgrade) {
            animateBadgeUpgrade()
        }

        updateMonthlyChart()
        updateHeatmap()
    }

    private fun updateBadgeVisual(badgeTier: Int, score: Int, isTierUpgrade: Boolean) {
        val starTextRes = when (badgeTier) {
            4 -> R.string.badge_star_level_4
            3 -> R.string.badge_star_level_3
            2 -> R.string.badge_star_level_2
            1 -> R.string.badge_star_level_1
            else -> R.string.badge_star_level_0
        }
        textBadgeStars.text = getString(starTextRes)

        val iconRes = if (badgeTier > 0) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        imageBadgeIcon.setImageResource(iconRes)

        val tintColor = when (badgeTier) {
            4 -> Color.parseColor("#F9A825")
            3 -> Color.parseColor("#FBC02D")
            2 -> Color.parseColor("#9CCC65")
            1 -> Color.parseColor("#81C784")
            else -> Color.parseColor("#90A4AE")
        }
        imageBadgeIcon.imageTintList = ColorStateList.valueOf(tintColor)

        val fxKey = key(selectedDay, "badge_icon_fx_shown")
        val alreadyShown = prefs.getBoolean(fxKey, false)
        val shouldPulse = isTierUpgrade || (badgeTier > 0 && score >= 80 && !alreadyShown)

        if (shouldPulse) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(imageBadgeIcon, "scaleX", 1f, 1.18f, 1f),
                    ObjectAnimator.ofFloat(imageBadgeIcon, "scaleY", 1f, 1.18f, 1f),
                    ObjectAnimator.ofFloat(imageBadgeIcon, "rotation", 0f, 8f, -8f, 0f)
                )
                duration = 520L
                start()
            }
        }

        if (badgeTier > 0 && score >= 80) {
            if (!alreadyShown) {
                prefs.edit().putBoolean(fxKey, true).apply()
            }
        } else if (alreadyShown) {
            prefs.edit().putBoolean(fxKey, false).apply()
        }
    }

    private fun maybeShowHighScoreOverlay(score: Int) {
        val fxKey = key(selectedDay, "high_score_fx_shown")
        val alreadyShown = prefs.getBoolean(fxKey, false)
        if (score >= 80 && !alreadyShown) {
            showHighScoreOverlay(score)
            prefs.edit().putBoolean(fxKey, true).apply()
            return
        }
        if (score < 80 && alreadyShown) {
            prefs.edit().putBoolean(fxKey, false).apply()
        }
    }

    private fun showHighScoreOverlay(score: Int) {
        badgeOverlay.removeAllViews()
        val density = resources.displayMetrics.density
        val overlayWidth = if (badgeOverlay.width > 0) badgeOverlay.width else resources.displayMetrics.widthPixels
        val overlayHeight = if (badgeOverlay.height > 0) badgeOverlay.height else resources.displayMetrics.heightPixels
        val centerX = overlayWidth / 2f
        val centerY = overlayHeight * 0.34f

        val badgeTitle = TextView(this).apply {
            text = getString(R.string.high_score_overlay_title)
            setTextColor(Color.parseColor("#FFF7CC"))
            textSize = 22f
            setShadowLayer(12f, 0f, 0f, Color.parseColor("#AA000000"))
            alpha = 0f
            translationY = 20f * density
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            x = centerX - 140f * density
            y = centerY - 80f * density
        }
        badgeOverlay.addView(badgeTitle)

        val badgeSubtitle = TextView(this).apply {
            text = getString(R.string.high_score_overlay_subtitle, score)
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 16f
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#AA000000"))
            alpha = 0f
            translationY = 16f * density
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            x = centerX - 96f * density
            y = centerY - 46f * density
        }
        badgeOverlay.addView(badgeSubtitle)

        repeat(10) { index ->
            val starSize = ((22 + (index % 3) * 8) * density).toInt()
            val startX = centerX + Random.nextInt(-170, 171) * density
            val startY = centerY + Random.nextInt(-36, 76) * density
            val star = ImageView(this).apply {
                setImageResource(android.R.drawable.btn_star_big_on)
                imageTintList = ColorStateList.valueOf(Color.parseColor("#FFD54F"))
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(starSize, starSize)
                x = startX
                y = startY
            }
            badgeOverlay.addView(star)

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(star, "alpha", 0f, 1f, 0f),
                    ObjectAnimator.ofFloat(star, "translationY", 0f, -(120f + Random.nextFloat() * 280f) * density),
                    ObjectAnimator.ofFloat(star, "translationX", 0f, Random.nextInt(-100, 101) * density),
                    ObjectAnimator.ofFloat(star, "rotation", 0f, Random.nextInt(120, 361).toFloat()),
                    ObjectAnimator.ofFloat(star, "scaleX", 0.7f, 1.2f, 0.9f),
                    ObjectAnimator.ofFloat(star, "scaleY", 0.7f, 1.2f, 0.9f)
                )
                startDelay = (index * 55).toLong()
                duration = 1600L
                start()
            }
        }

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(badgeTitle, "alpha", 0f, 1f, 0f),
                ObjectAnimator.ofFloat(badgeTitle, "translationY", 20f * density, -26f * density),
                ObjectAnimator.ofFloat(badgeSubtitle, "alpha", 0f, 1f, 0f),
                ObjectAnimator.ofFloat(badgeSubtitle, "translationY", 16f * density, -14f * density)
            )
            duration = 1700L
            start()
        }

        badgeOverlay.postDelayed({
            badgeOverlay.removeAllViews()
        }, 2200L)
    }

    private fun shareAchievementEmblem() {
        runCatching {
            val stats = buildEmblemStats()
            val emblemFile = createAchievementEmblemPng(stats)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                emblemFile
            )
            val caption = getString(
                R.string.share_emblem_caption,
                stats.totalScore,
                stats.bestStreak,
                stats.badgeLabel
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(getString(R.string.share_emblem_title), uri)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_emblem_title)))
        }.onFailure {
            Toast.makeText(this, getString(R.string.share_emblem_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildEmblemStats(): EmblemStats {
        val totalScore = (1..30).sumOf { calculateDayScore(it) }
        val bestStreak = getBestPerfectStreak()
        val badgeLabel = getBadgeByStreak(bestStreak)
        return EmblemStats(totalScore, bestStreak, badgeLabel)
    }

    private fun getBestPerfectStreak(): Int {
        var best = 0
        var current = 0
        for (day in 1..30) {
            if (isPerfectDay(day)) {
                current++
                best = max(best, current)
            } else {
                current = 0
            }
        }
        return best
    }

    private fun createAchievementEmblemPng(stats: EmblemStats): File {
        val width = 1080
        val height = 1920
        val scale = minOf(width / 1080f, height / 1920f)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                Color.parseColor("#0B3D2E"),
                Color.parseColor("#1F6F50"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66FFD89B")
        }
        canvas.drawCircle(width * 0.15f, height * 0.12f, emblemPx(140f, scale), glowPaint)
        canvas.drawCircle(width * 0.85f, height * 0.2f, emblemPx(90f, scale), glowPaint)

        val cardRect = RectF(
            emblemPx(56f, scale),
            emblemPx(180f, scale),
            width - emblemPx(56f, scale),
            height - emblemPx(220f, scale)
        )
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F9FFF8")
        }
        canvas.drawRoundRect(cardRect, emblemPx(28f, scale), emblemPx(28f, scale), cardPaint)
        drawEmblemAchievementDecor(canvas, cardRect, scale, stats.totalScore)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E4A36")
            textSize = emblemPx(19f, scale)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E5A48")
            textSize = emblemPx(14f, scale)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A6A5A")
            textSize = emblemPx(14f, scale)
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F362A")
            textSize = emblemPx(34f, scale)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val badgeValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F362A")
            textSize = emblemPx(24f, scale)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#567465")
            textSize = emblemPx(12f, scale)
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val hasHighScoreDecor = stats.totalScore >= 2000
        var y = cardRect.top + emblemPx(if (hasHighScoreDecor) 248f else 88f, scale)
        canvas.drawText(getString(R.string.emblem_heading), centerX, y, titlePaint)
        y += emblemPx(42f, scale)
        canvas.drawText(getString(R.string.emblem_subheading), centerX, y, subtitlePaint)

        y += emblemPx(110f, scale)
        canvas.drawText(getString(R.string.emblem_total_score_label), centerX, y, labelPaint)
        y += emblemPx(58f, scale)
        canvas.drawText("${stats.totalScore}/3000", centerX, y, valuePaint)

        y += emblemPx(108f, scale)
        canvas.drawText(getString(R.string.emblem_best_streak_label), centerX, y, labelPaint)
        y += emblemPx(58f, scale)
        canvas.drawText("${stats.bestStreak} hari", centerX, y, valuePaint)

        y += emblemPx(108f, scale)
        canvas.drawText(getString(R.string.emblem_badge_label), centerX, y, labelPaint)
        y += emblemPx(54f, scale)
        drawCenteredMultilineText(
            canvas,
            stats.badgeLabel,
            centerX,
            y,
            cardRect.width() - emblemPx(100f, scale),
            emblemPx(34f, scale),
            badgeValuePaint
        )

        canvas.drawText(getString(R.string.emblem_footer), centerX, cardRect.bottom - emblemPx(44f, scale), footerPaint)

        val emblemDir = File(cacheDir, "emblems").apply { mkdirs() }
        cleanupOldEmblems(emblemDir, keepLatest = 10)
        val emblemFile = File(emblemDir, "emblem-${System.currentTimeMillis()}.png")
        FileOutputStream(emblemFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return emblemFile
    }

    private fun drawEmblemAchievementDecor(canvas: Canvas, cardRect: RectF, scale: Float, totalScore: Int) {
        if (totalScore < 2000) return

        val centerX = cardRect.centerX()
        val topY = cardRect.top + emblemPx(38f, scale)

        if (totalScore >= 2700) {
            drawTrophyIcon(canvas, centerX, topY + emblemPx(12f, scale), scale)
        } else {
            drawMedalIcon(canvas, centerX, topY + emblemPx(10f, scale), scale)
        }

        val starCount = if (totalScore >= 2700) 8 else if (totalScore >= 2400) 6 else 4
        val starSize = emblemPx(36f, scale).toInt()
        val startX = cardRect.left + emblemPx(54f, scale)
        val endX = cardRect.right - emblemPx(54f, scale)
        val arcDepth = emblemPx(30f, scale)

        repeat(starCount) { index ->
            val factor = if (starCount == 1) 0f else index.toFloat() / (starCount - 1).toFloat()
            val x = startX + (endX - startX) * factor
            val y = topY + kotlin.math.abs(factor - 0.5f) * arcDepth + emblemPx(8f, scale)
            val drawable = ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_on)?.mutate() ?: return
            drawable.setTint(Color.parseColor("#F9A825"))
            drawable.alpha = (210 + (index % 3) * 15).coerceAtMost(255)
            drawable.setBounds(
                (x - starSize / 2f).toInt(),
                y.toInt(),
                (x + starSize / 2f).toInt(),
                (y + starSize).toInt()
            )
            drawable.draw(canvas)
        }
    }

    private fun drawTrophyIcon(canvas: Canvas, centerX: Float, topY: Float, scale: Float) {
        val cupWidth = emblemPx(112f, scale)
        val cupHeight = emblemPx(86f, scale)
        val stemHeight = emblemPx(34f, scale)
        val baseWidth = emblemPx(88f, scale)
        val baseHeight = emblemPx(20f, scale)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F9A825") }
        val goldDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C17900") }
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F9A825")
            style = Paint.Style.STROKE
            strokeWidth = emblemPx(8f, scale)
        }

        val cupRect = RectF(
            centerX - cupWidth / 2f,
            topY,
            centerX + cupWidth / 2f,
            topY + cupHeight
        )
        canvas.drawRoundRect(cupRect, emblemPx(18f, scale), emblemPx(18f, scale), goldPaint)

        val leftHandleRect = RectF(
            cupRect.left - emblemPx(30f, scale),
            cupRect.top + emblemPx(14f, scale),
            cupRect.left + emblemPx(8f, scale),
            cupRect.top + emblemPx(56f, scale)
        )
        val rightHandleRect = RectF(
            cupRect.right - emblemPx(8f, scale),
            cupRect.top + emblemPx(14f, scale),
            cupRect.right + emblemPx(30f, scale),
            cupRect.top + emblemPx(56f, scale)
        )
        canvas.drawArc(leftHandleRect, 110f, 230f, false, handlePaint)
        canvas.drawArc(rightHandleRect, -160f, 230f, false, handlePaint)

        val stemRect = RectF(
            centerX - emblemPx(14f, scale),
            cupRect.bottom - emblemPx(4f, scale),
            centerX + emblemPx(14f, scale),
            cupRect.bottom + stemHeight
        )
        canvas.drawRoundRect(stemRect, emblemPx(8f, scale), emblemPx(8f, scale), goldDarkPaint)

        val baseRect = RectF(
            centerX - baseWidth / 2f,
            stemRect.bottom - emblemPx(2f, scale),
            centerX + baseWidth / 2f,
            stemRect.bottom + baseHeight
        )
        canvas.drawRoundRect(baseRect, emblemPx(8f, scale), emblemPx(8f, scale), goldPaint)
    }

    private fun drawMedalIcon(canvas: Canvas, centerX: Float, topY: Float, scale: Float) {
        val ribbonWidth = emblemPx(18f, scale)
        val ribbonHeight = emblemPx(44f, scale)
        val medalRadius = emblemPx(40f, scale)

        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
        val ribbonPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0") }
        val medalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F9A825") }
        val medalInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }

        canvas.drawRect(centerX - ribbonWidth - emblemPx(6f, scale), topY, centerX - emblemPx(6f, scale), topY + ribbonHeight, ribbonPaint)
        canvas.drawRect(centerX + emblemPx(6f, scale), topY, centerX + ribbonWidth + emblemPx(6f, scale), topY + ribbonHeight, ribbonPaint2)

        val medalCy = topY + ribbonHeight + medalRadius
        canvas.drawCircle(centerX, medalCy, medalRadius, medalPaint)
        canvas.drawCircle(centerX, medalCy, medalRadius - emblemPx(9f, scale), medalInnerPaint)

        val star = Path().apply {
            val rOuter = medalRadius - emblemPx(15f, scale)
            val rInner = rOuter * 0.46f
            for (i in 0..9) {
                val angle = Math.toRadians((i * 36.0) - 90.0)
                val r = if (i % 2 == 0) rOuter else rInner
                val x = centerX + (kotlin.math.cos(angle) * r).toFloat()
                val y = medalCy + (kotlin.math.sin(angle) * r).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C17900") }
        canvas.drawPath(star, starPaint)
    }

    private fun cleanupOldEmblems(directory: File, keepLatest: Int) {
        val files = directory.listFiles { file ->
            file.isFile && file.name.startsWith("emblem-") && file.extension.equals("png", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        files.drop(keepLatest).forEach { oldFile ->
            runCatching { oldFile.delete() }
        }
    }

    private fun drawCenteredMultilineText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint
    ) {
        val words = text.trim().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return

        val lines = mutableListOf<String>()
        var currentLine = ""
        words.forEach { word ->
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth || currentLine.isBlank()) {
                currentLine = candidate
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotBlank()) {
            lines.add(currentLine)
        }

        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, centerX, y, paint)
            y += lineHeight
        }
    }

    private fun emblemPx(value: Float, scale: Float): Float {
        return value * scale
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

        val puasaDone = isPuasaValidForPerfectDay(day)
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
        val puasaDone = isPuasaCountedForScoreForDay(day)
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
            val puasaScore = if (isPuasaCountedForScoreForDay(day)) 100f else 0f
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

    private fun loadPrayerTimesFromCsv(assetFileName: String) {
        val loaded = mutableListOf<PrayerTimes>()
        val selectedLoadSuccess = runCatching {
            assets.open(assetFileName).bufferedReader().useLines { lines ->
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
            true
        }.getOrElse { false }

        if (!selectedLoadSuccess && assetFileName != CALENDAR_ASSET_KHGT) {
            runCatching {
                assets.open(CALENDAR_ASSET_KHGT).bufferedReader().useLines { lines ->
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

    private fun getSelectedAlasanTidakPuasa(): String {
        return when (groupAlasanTidakPuasa.checkedRadioButtonId) {
            radioAlasanSakit.id -> "sakit"
            radioAlasanSafar.id -> "safar"
            radioAlasanHaid.id -> "haid"
            else -> ""
        }
    }

    private fun getSelectedAlasanTidakPuasaLabel(): String {
        return when (groupAlasanTidakPuasa.checkedRadioButtonId) {
            radioAlasanSakit.id -> getString(R.string.alasan_sakit)
            radioAlasanSafar.id -> getString(R.string.alasan_safar)
            radioAlasanHaid.id -> getString(R.string.alasan_haid)
            else -> ""
        }
    }

    private fun updateAlasanTidakPuasaVisibility() {
        val visibility = if (groupPuasa.checkedRadioButtonId == radioTidakPuasa.id) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        findViewById<TextView>(R.id.textAlasanTidakPuasa).visibility = visibility
        groupAlasanTidakPuasa.visibility = visibility
    }

    private fun isPuasaCountedForScoreForCurrentDay(): Boolean {
        val puasaValue = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> "puasa"
            radioTidakPuasa.id -> "tidak"
            else -> ""
        }
        return isPuasaCountedForScore(puasaValue)
    }

    private fun isPuasaCountedForScoreForDay(day: Int): Boolean {
        val puasaValue = prefs.getString(key(day, "puasa"), "").orEmpty()
        return isPuasaCountedForScore(puasaValue)
    }

    private fun isPuasaCountedForScore(puasaValue: String): Boolean {
        return puasaValue == "puasa"
    }

    private fun isPuasaValidForPerfectDay(day: Int): Boolean {
        val puasaValue = prefs.getString(key(day, "puasa"), "").orEmpty()
        val alasanTidakPuasa = prefs.getString(key(day, "alasan_tidak_puasa"), "").orEmpty()
        return puasaValue == "puasa" || (puasaValue == "tidak" && alasanTidakPuasa.isNotBlank())
    }

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

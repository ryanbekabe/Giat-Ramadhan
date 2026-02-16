package com.hanyajasa.aplikasigiatramadhan

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        bindViews()
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
    }

    private fun setupDaySpinner() {
        val days = (1..30).map { "Hari $it Ramadhan" }
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        spinnerDay.setSelection(0)
        spinnerDay.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedDay = position + 1
                loadDayData(selectedDay)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No-op
            }
        })
    }

    private fun setupListeners() {
        val sholatChecks = listOf(checkSubuh, checkDzuhur, checkAshar, checkMaghrib, checkIsya)

        sholatChecks.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                if (isUpdatingUi) return@setOnCheckedChangeListener
                saveDayData()
                updateSummary()
            }
        }

        groupPuasa.setOnCheckedChangeListener { _, _ ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            saveDayData()
            updateSummary()
        }
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
            .apply()
    }

    private fun updateSummary() {
        val sholatDone = listOf(
            checkSubuh.isChecked,
            checkDzuhur.isChecked,
            checkAshar.isChecked,
            checkMaghrib.isChecked,
            checkIsya.isChecked
        ).count { it }

        val puasaText = when (groupPuasa.checkedRadioButtonId) {
            radioPuasa.id -> getString(R.string.status_puasa)
            radioTidakPuasa.id -> getString(R.string.status_tidak_puasa)
            else -> getString(R.string.status_belum)
        }

        textSummary.text = getString(R.string.summary_format, sholatDone, puasaText)
    }

    private fun key(day: Int, field: String): String = "day_${day}_$field"
}

package com.muratcan.apps.petvaccinetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.util.NotificationHelper
import com.muratcan.apps.petvaccinetracker.util.getParcelableExtraCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AddVaccineActivity : AppCompatActivity() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var dateAdministeredInput: EditText
    private lateinit var vaccineTypeInput: AutoCompleteTextView
    private lateinit var recurringPeriodInput: AutoCompleteTextView
    private lateinit var recurringOptionsContainer: View
    private lateinit var database: AppDatabase
    private var petId: Long = -1
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vaccine)

        // Initialize views
        nameInput = findViewById(R.id.vaccine_name_input)
        notesInput = findViewById(R.id.vaccine_notes_input)
        dateAdministeredInput = findViewById(R.id.date_administered_input)
        vaccineTypeInput = findViewById(R.id.vaccine_type_input)
        recurringPeriodInput = findViewById(R.id.recurring_period_input)
        recurringOptionsContainer = findViewById(R.id.recurring_options_container)
        val saveButton = findViewById<Button>(R.id.save_vaccine_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)

        // Get pet ID and vaccine from intent
        petId = intent.getLongExtra("pet_id", -1)
        val vaccine = intent.getParcelableExtraCompat<Vaccine>("vaccine")

        if (petId == -1L) {
            Toast.makeText(this, R.string.error_pet_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Set up date picker
        dateAdministeredInput.setOnClickListener { showDatePicker(dateAdministeredInput) }

        // Set up vaccine type dropdown
        val vaccineTypes = resources.getStringArray(R.array.recurring_options)
        val vaccineTypeAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, vaccineTypes
        )
        vaccineTypeInput.setAdapter(vaccineTypeAdapter)

        // Set up recurring period dropdown
        val recurringPeriods = resources.getStringArray(R.array.months_array)
        val recurringPeriodAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, recurringPeriods
        )
        recurringPeriodInput.setAdapter(recurringPeriodAdapter)

        // Handle vaccine type selection
        vaccineTypeInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedType = parent.getItemAtPosition(position).toString()
            if (selectedType == getString(R.string.recurring)) {
                recurringOptionsContainer.visibility = View.VISIBLE
                if (recurringPeriodInput.text.toString().isEmpty()) {
                    recurringPeriodInput.setText(getString(R.string.months_format, 1), false)
                }
            } else {
                recurringOptionsContainer.visibility = View.GONE
            }
        }

        // Set up save button with correct text and icon
        if (vaccine != null) {
            saveButton.text = getString(R.string.edit_vaccine)
            saveButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_save, 0, 0, 0)
        } else {
            saveButton.text = getString(R.string.save_vaccine)
            saveButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0)
        }

        // Load vaccine details if editing
        if (vaccine != null) {
            nameInput.setText(vaccine.name)
            notesInput.setText(vaccine.notes)

            vaccine.dateAdministered?.let { administered ->
                if (administered.time > 0) {
                    dateAdministeredInput.setText(dateFormat.format(administered))
                }
            }

            // Set default type to "One Time Only" if not recurring
            val isRecurring = vaccine.nextDueDate?.time ?: 0 > 0
            vaccineTypeInput.setText(getString(R.string.one_time_only), false)

            if (isRecurring && vaccine.dateAdministered?.time ?: 0 > 0) {
                vaccineTypeInput.setText(getString(R.string.recurring), false)
                recurringOptionsContainer.visibility = View.VISIBLE

                // Calculate months between administered and next due
                val administeredCal = Calendar.getInstance().apply {
                    time = vaccine.dateAdministered!!
                }
                val nextDueCal = Calendar.getInstance().apply {
                    time = vaccine.nextDueDate!!
                }

                val months =
                    (nextDueCal.get(Calendar.YEAR) - administeredCal.get(Calendar.YEAR)) * 12 +
                            (nextDueCal.get(Calendar.MONTH) - administeredCal.get(Calendar.MONTH))

                if (months > 0 && months <= 12) {
                    recurringPeriodInput.setText(getString(R.string.months_format, months), false)
                }
            }
        } else {
            // For new vaccines, default to "One Time Only"
            vaccineTypeInput.setText(getString(R.string.one_time_only), false)
        }

        // Set up save button
        saveButton.setOnClickListener { saveVaccine() }

        // Set up cancel button
        cancelButton.setOnClickListener { confirmExit() }

        // Set up action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(if (vaccine != null) R.string.edit_vaccine else R.string.add_vaccine)
        }

        // Add back pressed callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExit()
            }
        })
    }

    private fun showDatePicker(dateInput: EditText) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }

            // Validate that selected date is not in the future
            if (calendar.time.after(Date())) {
                Toast.makeText(this, R.string.date_future_error, Toast.LENGTH_SHORT).show()
                return@OnDateSetListener
            }

            dateInput.setText(dateFormat.format(calendar.time))
        }

        // Set max date to today
        val dialog = DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun saveVaccine() {
        val name = nameInput.text?.toString()?.trim() ?: ""
        val notes = notesInput.text?.toString()?.trim() ?: ""
        val dateAdministered = dateAdministeredInput.text.toString().trim()
        val vaccineType = vaccineTypeInput.text.toString().trim()

        if (name.isEmpty()) {
            nameInput.error = getString(R.string.vaccine_name_required)
            return
        }

        try {
            val administered =
                if (dateAdministered.isEmpty()) null else dateFormat.parse(dateAdministered)
            var nextDue: Date? = null
            var isRecurring = false
            var recurrenceMonths = 0

            if (administered != null && vaccineType == getString(R.string.recurring)) {
                val recurringPeriod = recurringPeriodInput.text.toString()
                if (recurringPeriod.isEmpty()) {
                    recurringPeriodInput.error = getString(R.string.recurring_period_required)
                    return
                }

                // Parse the number of months from the recurring period (e.g., "3 months" -> 3)
                recurrenceMonths = recurringPeriod.split(" ")[0].toInt()
                isRecurring = true

                // Calculate next due date
                val nextDueCalendar = Calendar.getInstance().apply {
                    time = administered
                    add(Calendar.MONTH, recurrenceMonths)
                }
                nextDue = nextDueCalendar.time
            }

            val existingVaccine = intent.getParcelableExtraCompat<Vaccine>("vaccine")
            val vaccineToSave = existingVaccine ?: Vaccine()

            vaccineToSave.apply {
                this.name = name
                this.notes = notes
                this.dateAdministered = administered
                this.nextDueDate = nextDue
                this.petId = this@AddVaccineActivity.petId
                this.isRecurring = isRecurring
                this.recurrenceMonths = recurrenceMonths
            }

            thread {
                try {
                    if (vaccineToSave.id == 0L) {
                        // New vaccine
                        val vaccineId = database.vaccineDao().insert(vaccineToSave)
                        vaccineToSave.id = vaccineId
                    } else {
                        // Update existing vaccine
                        database.vaccineDao().update(vaccineToSave)
                    }

                    // Schedule notification if there's a next due date
                    nextDue?.let { dueDate ->
                        // Get pet name for notification
                        val pet = database.petDao().getPetById(petId)
                        pet?.let { petData ->
                            val notifDateFormat =
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            NotificationHelper.scheduleNotification(
                                this@AddVaccineActivity,
                                petData.name,
                                name,
                                notifDateFormat.format(dueDate),
                                petId,
                                vaccineToSave.id
                            )
                        }
                    }

                    val isNewVaccine = existingVaccine == null
                    runOnUiThread {
                        val messageRes =
                            if (isNewVaccine) R.string.vaccine_added else R.string.vaccine_updated
                        Toast.makeText(this@AddVaccineActivity, messageRes, Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddVaccineActivity,
                            R.string.error_saving_vaccine,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_invalid_date, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val name = nameInput.text?.toString()?.trim() ?: ""
        val notes = notesInput.text?.toString()?.trim() ?: ""
        val dateAdministered = dateAdministeredInput.text.toString().trim()
        val vaccineType = vaccineTypeInput.text.toString().trim()

        return name.isNotEmpty() || notes.isNotEmpty() || dateAdministered.isNotEmpty() || vaccineType.isNotEmpty()
    }

    private fun confirmExit() {
        if (hasUnsavedChanges()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard) { _, _ -> finish() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                confirmExit()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
} 
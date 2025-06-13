package com.muratcan.apps.petvaccinetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.muratcan.apps.petvaccinetracker.adapter.VaccineAdapter
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.util.ImageUtils
import com.muratcan.apps.petvaccinetracker.util.getParcelableExtraCompat
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel
import timber.log.Timber
import java.text.DateFormat
import kotlin.concurrent.thread

class PetDetailActivity : AppCompatActivity(), VaccineAdapter.OnVaccineClickListener {
    private var pet: Pet? = null
    private lateinit var petImageView: ImageView
    private lateinit var petNameTextView: TextView
    private lateinit var petTypeChip: Chip
    private lateinit var petBreedChip: Chip
    private lateinit var petBirthDateTextView: TextView
    private lateinit var vaccineRecyclerView: RecyclerView
    private lateinit var vaccineAdapter: VaccineAdapter
    private lateinit var addVaccineFab: ExtendedFloatingActionButton
    private lateinit var emptyView: View
    private lateinit var viewModel: PetViewModel
    private lateinit var dateFormat: DateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        pet = intent.getParcelableExtraCompat<Pet>("pet")

        if (pet == null) {
            finish()
            return
        }

        // Initialize views and setup
        initViews()
        setupActionBar()
        setupRecyclerView()
        setupFab()
        setupBackPressedCallback()

        // Initialize ViewModel and observe changes
        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        observeViewModel()

        // Load initial data
        updatePetDetails(pet!!)
        viewModel.loadVaccines(pet!!.id)
    }

    private fun initViews() {
        petImageView = findViewById(R.id.petImageView)
        petNameTextView = findViewById(R.id.petNameTextView)
        petTypeChip = findViewById(R.id.petTypeChip)
        petBreedChip = findViewById(R.id.petBreedChip)
        petBirthDateTextView = findViewById(R.id.petBirthDateTextView)
        vaccineRecyclerView = findViewById(R.id.vaccineRecyclerView)
        addVaccineFab = findViewById(R.id.addVaccineFab)
        emptyView = findViewById(R.id.emptyView)
        dateFormat = android.text.format.DateFormat.getDateFormat(this)

        // Show empty view by default and hide recycler view
        vaccineRecyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.alpha = 1f
        addVaccineFab.hide()

        // Set up empty state add button
        findViewById<View>(R.id.emptyStateAddVaccineButton).setOnClickListener { v ->
            val intent = Intent(this, AddVaccineActivity::class.java).apply {
                putExtra("pet_id", pet!!.id)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, v, "shared_element_container"
            )
            startActivity(intent, options.toBundle())
        }

        // Set up FAB behavior
        vaccineRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    addVaccineFab.shrink()
                } else if (dy < 0) {
                    addVaccineFab.extend()
                }
            }
        })
    }

    private fun setupActionBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = pet!!.name

        findViewById<View>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<View>(R.id.editButton).setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java).apply {
                putExtra("pet", pet)
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.deleteButton).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_pet_confirmation)
                .setMessage(R.string.delete_pet_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Delete pet
                    thread {
                        val db = AppDatabase.getDatabase(this@PetDetailActivity)
                        // First delete all vaccines for this pet
                        db.vaccineDao().deleteAllForPet(pet!!.id)
                        // Then delete the pet
                        db.petDao().delete(pet!!)
                        runOnUiThread {
                            Toast.makeText(
                                this@PetDetailActivity,
                                R.string.pet_deleted,
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun setupRecyclerView() {
        vaccineRecyclerView.layoutManager = LinearLayoutManager(this)
        vaccineAdapter = VaccineAdapter(mutableListOf(), this)
        vaccineRecyclerView.adapter = vaccineAdapter
    }

    private fun setupFab() {
        addVaccineFab.setOnClickListener { v ->
            val intent = Intent(this@PetDetailActivity, AddVaccineActivity::class.java).apply {
                putExtra("pet_id", pet!!.id)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, addVaccineFab, "shared_element_container"
            )
            startActivity(intent, options.toBundle())
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                supportFinishAfterTransition()
            }
        })
    }

    private fun observeViewModel() {
        // Observe StateFlow values using DefaultLifecycleObserver
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Observe vaccines using LiveData
                viewModel.getVaccinesLiveData().observe(this@PetDetailActivity, ::updateVaccineList)

                // Observe loading state using LiveData
                viewModel.getIsLoadingLiveData()
                    .observe(this@PetDetailActivity, ::updateLoadingState)

                // Observe error state using LiveData
                viewModel.getErrorLiveData().observe(this@PetDetailActivity, ::showError)

                // Observe current pet using LiveData
                viewModel.getCurrentPetLiveData().observe(this@PetDetailActivity) { pet: Pet? ->
                    pet?.let { updatePetDetails(it) }
                }
            }
        })
    }

    private fun updateVaccineList(vaccines: List<Vaccine>?) {
        vaccines?.let {
            vaccineAdapter.updateVaccines(it)
            updateEmptyView(it.isEmpty())
        }
    }

    private fun updateLoadingState(isLoading: Boolean?) {
        // Update loading state UI
        if (isLoading == true) {
            // Show loading indicator
        } else {
            // Hide loading indicator
        }
    }

    private fun showError(error: String?) {
        if (!error.isNullOrEmpty()) {
            Snackbar.make(vaccineRecyclerView, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            // Show empty view with animation
            emptyView.visibility = View.VISIBLE
            emptyView.alpha = 1f
            vaccineRecyclerView.visibility = View.GONE
            addVaccineFab.hide()
        } else {
            // Show recycler view with animation
            vaccineRecyclerView.alpha = 0f
            vaccineRecyclerView.visibility = View.VISIBLE
            vaccineRecyclerView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            emptyView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { emptyView.visibility = View.GONE }
                .start()
            addVaccineFab.show()
            addVaccineFab.extend()
        }
    }

    override fun onResume() {
        super.onResume()
        // Force a refresh of the vaccines
        viewModel.loadVaccines(pet!!.id)
        viewModel.loadPetById(pet!!.id)
    }

    override fun onVaccineClick(vaccine: Vaccine) {
        val intent = Intent(this, AddVaccineActivity::class.java).apply {
            putExtra("vaccine", vaccine)
            putExtra("pet_id", pet!!.id)
        }
        startActivity(intent)
    }

    override fun onVaccineDelete(vaccine: Vaccine) {
        thread {
            AppDatabase.getDatabase(this).vaccineDao().delete(vaccine)
            runOnUiThread {
                Snackbar.make(vaccineRecyclerView, R.string.vaccine_deleted, Snackbar.LENGTH_SHORT)
                    .show()
                viewModel.refreshVaccines(pet!!.id)
            }
        }
    }

    private fun updatePetDetails(updatedPet: Pet) {
        pet = updatedPet
        // Update UI elements
        if (!updatedPet.imageUri.isNullOrEmpty()) {
            try {
                ImageUtils.loadImage(this, Uri.parse(updatedPet.imageUri), petImageView)
                Timber.d("Loading image URI in detail: ${updatedPet.imageUri}")
            } catch (e: Exception) {
                Timber.e(e, "Error loading pet image in detail view")
                petImageView.setImageResource(R.drawable.ic_pet_placeholder)
            }
        } else {
            petImageView.setImageResource(R.drawable.ic_pet_placeholder)
        }

        petNameTextView.text = updatedPet.name
        petTypeChip.text = updatedPet.type
        petBreedChip.text = updatedPet.breed

        // Update birth date and age
        updatedPet.birthDate?.let { birthDate ->
            petBirthDateTextView.text =
                getString(R.string.born_on_date, dateFormat.format(birthDate))

            // Update age chip
            val petAgeChip = findViewById<Chip>(R.id.petAgeChip)
            petAgeChip?.let { ageChip ->
                val ageInMillis = System.currentTimeMillis() - birthDate.time
                val ageInYears = (ageInMillis / (1000L * 60 * 60 * 24 * 365)).toInt()
                val ageInMonths = (ageInMillis / (1000L * 60 * 60 * 24 * 30) % 12).toInt()

                val ageText = when {
                    ageInYears > 0 -> {
                        val yearsText =
                            resources.getQuantityString(R.plurals.years_old, ageInYears, ageInYears)
                        if (ageInMonths > 0) {
                            val monthsText = resources.getQuantityString(
                                R.plurals.months_old,
                                ageInMonths,
                                ageInMonths
                            )
                            "$yearsText $monthsText"
                        } else {
                            yearsText
                        }
                    }

                    else -> resources.getQuantityString(
                        R.plurals.months_old,
                        ageInMonths,
                        ageInMonths
                    )
                }
                ageChip.text = ageText
            }
        }
    }
} 
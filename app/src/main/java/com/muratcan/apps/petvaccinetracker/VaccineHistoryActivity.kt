package com.muratcan.apps.petvaccinetracker

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.muratcan.apps.petvaccinetracker.adapter.VaccineHistoryAdapter
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel

class VaccineHistoryActivity : AppCompatActivity(),
    VaccineHistoryAdapter.OnHistoryItemClickListener {
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: VaccineHistoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var petFilterSpinner: Spinner
    private lateinit var viewModel: PetViewModel
    private var allPets: List<Pet>? = null
    private var selectedPetId = -1L // -1 means all pets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vaccine_history)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupSpinner()

        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        observeData()
    }

    private fun initViews() {
        historyRecyclerView = findViewById(R.id.history_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        petFilterSpinner = findViewById(R.id.pet_filter_spinner)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Vaccination History"
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = VaccineHistoryAdapter(this)
        historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@VaccineHistoryActivity)
        }
    }

    private fun setupSpinner() {
        petFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedPetId = if (position == 0) {
                    -1L // All pets
                } else {
                    allPets?.get(position - 1)?.id ?: -1L
                }
                loadHistoryData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeData() {
        // Load pets for filter spinner
        viewModel.getAllPets().observe(this) { pets: List<Pet> ->
            allPets = pets
            setupSpinnerData()
        }

        loadHistoryData()
    }

    private fun setupSpinnerData() {
        val spinnerItems = mutableListOf<String>().apply {
            add("All Pets")
            allPets?.forEach { pet ->
                add(pet.name)
            }
        }

        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, spinnerItems
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        petFilterSpinner.adapter = spinnerAdapter
    }

    private fun loadHistoryData() {
        val historyObserver = if (selectedPetId == -1L) {
            // Load all history
            viewModel.getVaccineHistory()
        } else {
            // Load history for specific pet
            viewModel.getVaccineHistoryForPet(selectedPetId)
        }

        historyObserver.observe(this) { historyItems: List<VaccineHistoryItem> ->
            updateHistoryDisplay(historyItems)
        }
    }

    private fun updateHistoryDisplay(historyItems: List<VaccineHistoryItem>) {
        if (historyItems.isEmpty()) {
            historyRecyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            historyRecyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            historyAdapter.updateHistory(historyItems)
        }
    }

    override fun onHistoryItemClick(item: VaccineHistoryItem) {
        // Open pet detail page
        val intent = Intent(this, PetDetailActivity::class.java).apply {
            putExtra("petId", item.petId)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
} 
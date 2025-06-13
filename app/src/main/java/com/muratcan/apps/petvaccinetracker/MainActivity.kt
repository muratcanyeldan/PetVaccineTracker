package com.muratcan.apps.petvaccinetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.muratcan.apps.petvaccinetracker.adapter.PetAdapter
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var petRecyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var addPetFab: ExtendedFloatingActionButton
    private lateinit var emptyView: View
    private lateinit var viewModel: PetViewModel
    private lateinit var searchView: SearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseHelper = FirebaseHelper()
        setupPermissions()
        initViews()
        setupRecyclerView()
        setupFab()
        setupSwipeRefresh()

        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        observeViewModel()
    }

    private fun setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Timber.d("Notification permission granted")
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Notifications are disabled. You won't receive vaccine reminders.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        // Check and request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initViews() {
        petRecyclerView = findViewById(R.id.petRecyclerView)
        addPetFab = findViewById(R.id.addPetFab)
        emptyView = findViewById(R.id.emptyView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup sort button
        val sortButton = findViewById<ImageButton>(R.id.sortButton)
        sortButton.setOnClickListener { v ->
            val popup = PopupMenu(this@MainActivity, sortButton)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)

            // Remove the search item from popup menu
            popup.menu.removeItem(R.id.action_search)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_sort_name -> {
                        Timber.d("Selected sort by name")
                        viewModel.sortByName()
                        true
                    }

                    R.id.action_sort_date -> {
                        Timber.d("Selected sort by date")
                        viewModel.sortByDate()
                        true
                    }

                    R.id.action_sort_none -> {
                        Timber.d("Selected clear sort")
                        viewModel.clearSort()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        // Setup search view
        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.filterPets(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.filterPets(newText)
                return true
            }
        })

        // Setup empty state add button
        findViewById<View>(R.id.emptyStateAddButton)?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddPetActivity::class.java)
            startActivity(intent)
        }

        // Setup logout button
        findViewById<View>(R.id.logoutButton).setOnClickListener { v ->
            // Show confirmation dialog
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirmation)
                .setMessage(R.string.logout_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    try {
                        // Sign out from Firebase
                        firebaseHelper.signOut()
                        Timber.d("User signed out successfully")

                        // Clear all activities and start LoginActivity
                        val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        // Kill the current task to prevent going back
                        finishAndRemoveTask()
                    } catch (e: Exception) {
                        Timber.e(e, "Error logging out: ${e.message}")
                        Snackbar.make(v, "Error logging out: ${e.message}", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPets()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the pet list when returning to this screen
        viewModel.refreshPets()
    }

    private fun setupRecyclerView() {
        petRecyclerView.layoutManager = LinearLayoutManager(this)
        petAdapter = PetAdapter(mutableListOf(), object : PetAdapter.OnPetClickListener {
            override fun onPetClick(pet: Pet) {
                val intent = Intent(this@MainActivity, PetDetailActivity::class.java).apply {
                    putExtra("pet", pet)
                }
                val transitionName = "pet_card_${pet.id}"
                val sharedView = petRecyclerView.findViewWithTag<View>(transitionName)
                if (sharedView != null) {
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@MainActivity,
                        sharedView,
                        transitionName
                    )
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
        })
        petRecyclerView.adapter = petAdapter
    }

    private fun setupFab() {
        addPetFab.setOnClickListener {
            val intent = Intent(this@MainActivity, AddPetActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Observe StateFlow values using DefaultLifecycleObserver
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Observe pets using LiveData
                viewModel.getPetsLiveData().observe(this@MainActivity, ::updatePetList)

                // Observe loading state using LiveData
                viewModel.getIsLoadingLiveData().observe(this@MainActivity, ::updateLoadingState)

                // Observe error state using LiveData
                viewModel.getErrorLiveData().observe(this@MainActivity, ::showError)
            }
        })
    }

    private fun updatePetList(pets: List<Pet>?) {
        pets?.let {
            petAdapter.updatePets(it)
            updateEmptyView(it.isEmpty())
        }
    }

    private fun updateLoadingState(isLoading: Boolean?) {
        if (isLoading == true) {
            // Show loading indicator
            swipeRefreshLayout.isRefreshing = true
            if (petAdapter.itemCount == 0) {
                emptyView.visibility = View.VISIBLE
                petRecyclerView.visibility = View.GONE
                addPetFab.hide()
            }
        } else {
            // Update visibility based on whether we have pets
            swipeRefreshLayout.isRefreshing = false
            updateEmptyView(petAdapter.itemCount == 0)
        }
    }

    private fun showError(error: String?) {
        if (!error.isNullOrEmpty()) {
            Snackbar.make(petRecyclerView, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            petRecyclerView.visibility = View.GONE
            addPetFab.hide()

            // Make sure the empty state button is visible
            findViewById<View>(R.id.emptyStateAddButton)?.apply {
                visibility = View.VISIBLE
            }

            emptyView.findViewById<TextView>(R.id.emptyTextView)?.apply {
                text = getString(R.string.no_pets_found)
            }
        } else {
            emptyView.visibility = View.GONE
            petRecyclerView.visibility = View.VISIBLE
            addPetFab.show()
            addPetFab.extend()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
} 
package com.muratcan.apps.petvaccinetracker

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.util.FirebaseHelper
import com.muratcan.apps.petvaccinetracker.util.ImageUtils
import com.muratcan.apps.petvaccinetracker.util.getParcelableExtraCompat
import com.muratcan.apps.petvaccinetracker.viewmodel.PetViewModel
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AddPetActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var nameLayout: TextInputLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var typeLayout: TextInputLayout
    private lateinit var typeInput: AutoCompleteTextView
    private lateinit var breedLayout: TextInputLayout
    private lateinit var breedInput: TextInputEditText
    private lateinit var birthDateLayout: TextInputLayout
    private lateinit var birthDateInput: TextInputEditText
    private lateinit var petImageView: ShapeableImageView
    private lateinit var addButton: MaterialButton
    private lateinit var viewModel: PetViewModel
    private lateinit var dateFormat: DateFormat
    private lateinit var firebaseHelper: FirebaseHelper

    private var selectedImageUri: Uri? = null
    private var isImageChanged = false
    private var selectedDate: Date? = null
    private var existingPet: Pet? = null
    private var photoUri: Uri? = null

    private val pickImage: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            handleImageResult(result.data?.data)
        }
    }

    private val takePhoto: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleImageResult(photoUri)
        }
    }

    private fun handleImageResult(sourceUri: Uri?) {
        sourceUri?.let { uri ->
            try {
                // Show preview immediately
                selectedImageUri = uri // Store the original URI for immediate preview

                // Reset image view styling for actual image
                petImageView.apply {
                    setBackgroundResource(0)
                    setPadding(0, 0, 0, 0)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    colorFilter = null
                }

                // Load the image immediately for preview
                ImageUtils.loadImage(this, selectedImageUri!!, petImageView)
                Timber.d("Showing preview of image: $selectedImageUri")

                // Copy the image to app storage in the background
                thread {
                    try {
                        val storedImagePath =
                            ImageUtils.copyImageToAppStorage(this@AddPetActivity, uri)
                        if (storedImagePath != null) {
                            val storedUri =
                                ImageUtils.getImageUri(this@AddPetActivity, storedImagePath)
                            if (storedUri != null) {
                                runOnUiThread {
                                    selectedImageUri =
                                        storedUri // Update the URI to the stored version
                                    Timber.d("Image stored successfully at: $storedUri")
                                }
                            } else {
                                Timber.e("Failed to get URI for stored image path: $storedImagePath")
                                runOnUiThread {
                                    Snackbar.make(
                                        petImageView,
                                        R.string.error_saving_image,
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Timber.e("Failed to copy image to app storage")
                            runOnUiThread {
                                Snackbar.make(
                                    petImageView,
                                    R.string.error_saving_image,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error storing image: ${e.message}")
                        runOnUiThread {
                            Snackbar.make(
                                petImageView,
                                R.string.error_saving_image,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                isImageChanged = true
            } catch (e: Exception) {
                Timber.e(e, "Error processing selected image: ${e.message}")
                Snackbar.make(petImageView, R.string.error_processing_image, Snackbar.LENGTH_LONG)
                    .show()
                resetToPlaceholder()
            }
        }
    }

    private fun resetToPlaceholder() {
        petImageView.apply {
            setBackgroundResource(R.color.md_theme_light_surfaceVariant)
            setPadding(32, 32, 32, 32)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_pet_placeholder)
            setColorFilter(resources.getColor(R.color.md_theme_light_outline, theme))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)

        Timber.d("AddPetActivity onCreate")

        firebaseHelper = FirebaseHelper()
        initViews()
        setupToolbar()
        setupTypeDropdown()
        setupDatePicker()
        setupImagePicker()

        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        observeViewModel()

        // Check if we're editing an existing pet
        intent?.let { intent ->
            if (intent.hasExtra("pet")) {
                try {
                    existingPet = intent.getParcelableExtraCompat<Pet>("pet")
                    Timber.d("Received pet for editing: ${existingPet?.name ?: "null"}")
                    existingPet?.let {
                        populateExistingPetData()
                    } ?: run {
                        Timber.e("Failed to get pet from intent")
                        showError(getString(R.string.error_pet_not_found))
                        finish()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error getting pet from intent")
                    showError(getString(R.string.error_pet_not_found))
                    finish()
                }
            } else {
                Timber.d("Creating new pet")
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        nameLayout = findViewById(R.id.nameLayout)
        nameInput = findViewById(R.id.nameInput)
        typeLayout = findViewById(R.id.typeLayout)
        typeInput = findViewById(R.id.typeInput)
        breedLayout = findViewById(R.id.breedLayout)
        breedInput = findViewById(R.id.breedInput)
        birthDateLayout = findViewById(R.id.birthDateLayout)
        birthDateInput = findViewById(R.id.birthDateInput)
        petImageView = findViewById(R.id.petImageView)
        addButton = findViewById(R.id.addButton)

        dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

        addButton.setOnClickListener { savePet() }

        // Setup cancel button
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)
        cancelButton.setOnClickListener { finish() }
    }

    private fun setupTypeDropdown() {
        val petTypes = resources.getStringArray(R.array.pet_types)
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, petTypes
        )
        typeInput.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        birthDateInput.apply {
            setOnClickListener { showDatePicker() }
            isFocusable = false
        }
    }

    private fun setupImagePicker() {
        petImageView.setOnClickListener { checkAndRequestPermissions(true) }
        val changeImageButton = findViewById<FloatingActionButton>(R.id.changeImageButton)
        changeImageButton.setOnClickListener { checkAndRequestPermissions(false) }
    }

    private fun checkAndRequestPermissions(isGallery: Boolean) {
        if (isGallery) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                launchImagePicker()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                launchCamera()
            }
        }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                val storageDir = File(filesDir, ImageUtils.IMAGES_DIR).apply {
                    if (!exists()) mkdirs()
                }
                Timber.d("Storage directory created at: ${storageDir.absolutePath}")

                val timeStamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                photoFile = File(storageDir, "JPEG_${timeStamp}.jpg")
                Timber.d("Photo file created at: ${photoFile.absolutePath}")
            } catch (e: Exception) {
                Timber.e(e, "Error creating image file")
            }

            // Continue only if the File was successfully created
            photoFile?.let { file ->
                try {
                    photoUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        file
                    )
                    Timber.d("FileProvider URI created: $photoUri")

                    intent.apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    takePhoto.launch(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error getting URI for photo file: ${e.message}")
                    Snackbar.make(
                        petImageView,
                        R.string.error_processing_image,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Timber.w("No camera app available")
            Snackbar.make(petImageView, R.string.error_no_camera, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0] == Manifest.permission.CAMERA) {
                    launchCamera()
                } else {
                    launchImagePicker()
                }
            } else {
                Snackbar.make(
                    petImageView,
                    R.string.permission_denied_message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                // Check if selected date is in the future
                if (selectedCal.after(Calendar.getInstance())) {
                    Snackbar.make(birthDateInput, R.string.future_date_error, Snackbar.LENGTH_LONG)
                        .show()
                    return@DatePickerDialog
                }

                selectedDate = selectedCal.time
                birthDateInput.setText(dateFormat.format(selectedDate!!))
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )

        // Set the maximum date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun populateExistingPetData() {
        val pet = existingPet ?: run {
            Timber.e("Existing pet is null in edit mode")
            showError(getString(R.string.error_pet_not_found))
            finish()
            return
        }

        Timber.d("Populating existing pet data: id=${pet.id}, name=${pet.name}")

        nameInput.setText(pet.name)
        typeInput.setText(pet.type)
        breedInput.setText(pet.breed)

        pet.birthDate?.let { birthDate ->
            selectedDate = birthDate
            birthDateInput.setText(dateFormat.format(selectedDate!!))
        } ?: Timber.w("Birth date is null for existing pet")

        if (!pet.imageUri.isNullOrEmpty()) {
            try {
                selectedImageUri = Uri.parse(pet.imageUri)
                // Reset image view styling for actual image
                petImageView.apply {
                    setBackgroundResource(0) // Remove background
                    setPadding(0, 0, 0, 0)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    colorFilter = null
                }

                ImageUtils.loadImage(this, selectedImageUri!!, petImageView)
                Timber.d("Loaded image URI in edit mode: $selectedImageUri")
            } catch (e: Exception) {
                Timber.e(e, "Error loading pet image in edit mode: ${e.message}")
                resetToPlaceholder()
            }
        } else {
            Timber.d("No image URI for existing pet")
            resetToPlaceholder()
        }

        addButton.setText(R.string.update_pet)
    }

    private fun observeViewModel() {
        // Observe StateFlow values using DefaultLifecycleObserver
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Observe loading state using LiveData
                viewModel.getIsLoadingLiveData().observe(this@AddPetActivity, ::updateLoadingState)

                // Observe error state using LiveData
                viewModel.getErrorLiveData().observe(this@AddPetActivity, ::showError)
            }
        })
    }

    private fun updateLoadingState(isLoading: Boolean?) {
        if (isLoading == true) {
            addButton.apply {
                isEnabled = false
                text = getString(R.string.saving)
            }
        } else {
            addButton.apply {
                isEnabled = true
                text = getString(if (existingPet != null) R.string.update_pet else R.string.add_pet)
            }
        }
    }

    private fun showError(error: String?) {
        if (!error.isNullOrEmpty()) {
            Snackbar.make(addButton, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun savePet() {
        if (!validateInputs()) {
            Timber.d("Input validation failed")
            return
        }

        val name = nameInput.text.toString().trim()
        val type = typeInput.text.toString().trim()
        val breed = breedInput.text.toString().trim()

        existingPet?.let { pet ->
            // Update existing pet
            Timber.d("Updating existing pet with ID: ${pet.id}")
            pet.apply {
                this.name = name
                this.type = type
                this.breed = breed
                this.birthDate = selectedDate ?: Date()
            }

            if (isImageChanged && selectedImageUri != null) {
                // Delete old image if it exists
                if (!pet.imageUri.isNullOrEmpty()) {
                    ImageUtils.deleteImage(this, pet.imageUri!!)
                }
                // Set the new image URI directly
                pet.imageUri = selectedImageUri.toString()
                Timber.d("Setting new image URI: ${selectedImageUri.toString()}")
            }

            viewModel.updatePet(pet)
            finish()
        } ?: run {
            // Create new pet
            Timber.d("Creating new pet with name: $name")
            val newPet = Pet().apply {
                this.name = name
                this.type = type
                this.breed = breed
                this.birthDate = selectedDate ?: Date()
            }

            val userId = firebaseHelper.getCurrentUserId()
            if (userId == null) {
                showError("Error: User not logged in")
                return
            }
            newPet.userId = userId

            selectedImageUri?.let { uri ->
                newPet.imageUri = uri.toString()
                Timber.d("Setting image URI for new pet: ${uri.toString()}")
            }

            viewModel.addPet(newPet)

            // Only finish if there's no error
            viewModel.getErrorLiveData().observe(this) { error: String? ->
                if (error == null) {
                    finish()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (TextUtils.isEmpty(nameInput.text)) {
            nameLayout.error = getString(R.string.required_field)
            Timber.w("Name field is empty")
            isValid = false
        } else {
            nameLayout.error = null
        }

        if (TextUtils.isEmpty(typeInput.text)) {
            typeLayout.error = getString(R.string.required_field)
            Timber.w("Type field is empty")
            isValid = false
        } else {
            typeLayout.error = null
        }

        if (TextUtils.isEmpty(breedInput.text)) {
            breedLayout.error = getString(R.string.required_field)
            Timber.w("Breed field is empty")
            isValid = false
        } else {
            breedLayout.error = null
        }

        if (selectedDate == null) {
            birthDateLayout.error = getString(R.string.required_field)
            Timber.w("Birth date is not selected")
            isValid = false
        } else {
            birthDateLayout.error = null
        }

        return isValid
    }
} 
package com.example.landmark_app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.landmark_app.databinding.FragmentEntryBinding
import com.example.landmark_app.model.Landmark
import com.example.landmark_app.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class EntryFragment : Fragment() {

    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private var pickedImageBitmap: Bitmap? = null
    private var editingLandmark: Landmark? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Detect edit mode
        arguments?.let {
            if (it.containsKey("id")) {
                editingLandmark = Landmark(
                    id = it.getInt("id"),
                    title = it.getString("title") ?: "",
                    latitude = it.getString("lat"),
                    longitude = it.getString("lon"),
                    image = (it.getString("image") ?: "").trim()
                )
            }
        }

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                pickedImageBitmap = loadBitmapFromUri(uri)
                binding.ivPreview.setImageBitmap(pickedImageBitmap)
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocation || coarseLocation) {
                 // Permission granted
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Prefill edit mode
        editingLandmark?.let { lm ->
            binding.etTitle.setText(lm.title)
            binding.etLat.setText(lm.latitude?.toString() ?: "")
            binding.etLon.setText(lm.longitude?.toString() ?: "")

            if (lm.image.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(lm.fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(binding.ivPreview)
            }

            binding.btnSubmit.text = "Update Landmark"
        }

        // Auto-fill location ONLY for new landmark
        if (editingLandmark == null) {
            detectCurrentLocation()
        }

        binding.ivPreview.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnUseCurrentLat.setOnClickListener { fetchCurrentLat() }
        binding.btnUseCurrentLon.setOnClickListener { fetchCurrentLon() }

        binding.btnSubmit.setOnClickListener { saveLandmark() }
    }

    @SuppressLint("MissingPermission")
    private fun detectCurrentLocation() {
        checkLocation {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    binding.etLat.setText(loc.latitude.toString())
                    binding.etLon.setText(loc.longitude.toString())
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: android.net.Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLat() {
        checkLocation {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { binding.etLat.setText(it.latitude.toString()) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLon() {
        checkLocation {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { binding.etLon.setText(it.longitude.toString()) }
            }
        }
    }

    private fun checkLocation(onGranted: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        onGranted()
    }

    private fun saveLandmark() {
        val title = binding.etTitle.text.toString().trim()
        val latStr = binding.etLat.text.toString().trim()
        val lonStr = binding.etLon.text.toString().trim()

        if (title.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = latStr.toDoubleOrNull()
        val lon = lonStr.toDoubleOrNull()

        if (lat == null || lon == null) {
            Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val api = RetrofitInstance.api

                val response = if (editingLandmark == null) {
                    // CREATE
                    if (pickedImageBitmap == null) {
                        Toast.makeText(requireContext(), "Image required for new landmark", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val imagePart = partFromBitmap(pickedImageBitmap!!)
                    api.createLandmark(
                        title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                        lat = latStr.toRequestBody("text/plain".toMediaTypeOrNull()),
                        lon = lonStr.toRequestBody("text/plain".toMediaTypeOrNull()),
                        image = imagePart
                    )
                } else {
                    // UPDATE
                    val landmarkId = editingLandmark!!.id
                    if (pickedImageBitmap != null) {
                        val imagePart = partFromBitmap(pickedImageBitmap!!)
                        val idBody = landmarkId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                        val latBody = latStr.toRequestBody("text/plain".toMediaTypeOrNull())
                        val lonBody = lonStr.toRequestBody("text/plain".toMediaTypeOrNull())

                        val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                        api.updateLandmarkWithImage(
                            method = methodBody,
                            id = idBody,
                            title = titleBody,
                            lat = latBody,
                            lon = lonBody,
                            image = imagePart
                        )
                    } else {
                        api.updateLandmarkNoImage(
                            id = landmarkId,
                            title = title,
                            lat = lat,
                            lon = lon
                        )
                    }
                }

                if (response.isSuccessful) {
                    setFragmentResult("landmarkSaved", bundleOf("refresh" to true))
                    Toast.makeText(requireContext(), "Success!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Operation failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun partFromBitmap(bitmap: Bitmap): MultipartBody.Part {
        val resized = resizeBitmapExact(bitmap)  // always 800×600
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()

        val req = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "upload.jpg", req)
    }

    private fun resizeBitmapExact(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, 800, 600, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

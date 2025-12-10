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
import androidx.fragment.app.Fragment
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
                // If the bundle contains lat/lon as Double, use getDouble. If String, use getString.
                // We'll try both to be safe or stick to the one we know the sender uses.
                // OverviewFragment now sends Double. RecordsFragment might send String if we kept it old way.
                // Let's implement safe retrieval.

                @Suppress("DEPRECATION")
                val latStr = if (it.get("lat") is Double) it.getDouble("lat").toString() else it.getString("lat") ?: ""
                @Suppress("DEPRECATION")
                val lonStr = if (it.get("lon") is Double) it.getDouble("lon").toString() else it.getString("lon") ?: ""

                editingLandmark = Landmark(
                    id = it.getInt("id"),
                    title = it.getString("title") ?: "",
                    latitude = latStr,
                    longitude = lonStr,
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
            binding.etLat.setText(lm.latitude)
            binding.etLon.setText(lm.longitude)

            if (lm.image.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(lm.fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(binding.ivPreview)
            }

            binding.btnSubmit.text = "Update Landmark"
        }

        binding.ivPreview.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnUseCurrentLat.setOnClickListener { fetchCurrentLat() }
        binding.btnUseCurrentLon.setOnClickListener { fetchCurrentLon() }

        binding.btnSubmit.setOnClickListener { saveLandmark() }
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
        val lat = binding.etLat.text.toString().trim()
        val lon = binding.etLon.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Required"
            return
        }
        
        if (lat.isEmpty()) {
            binding.etLat.error = "Required"
            return
        }
        
        if (lon.isEmpty()) {
            binding.etLon.error = "Required"
            return
        }

        if (editingLandmark == null && pickedImageBitmap == null) {
            Toast.makeText(requireContext(), "Please choose an image", Toast.LENGTH_SHORT).show()
            return
        }

        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat.toRequestBody("text/plain".toMediaTypeOrNull())
        val lonBody = lon.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null

        if (pickedImageBitmap != null) {
            val resized = resizeBitmap(pickedImageBitmap!!, 800, 600)
            val stream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            val req = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("image", "upload.jpg", req)
        }

        lifecycleScope.launch {
            try {
                val api = RetrofitInstance.api

                val response = if (editingLandmark == null) {
                    if (imagePart == null) {
                         Toast.makeText(requireContext(), "Image required", Toast.LENGTH_SHORT).show()
                         return@launch
                    }
                    api.createLandmark(titleBody, latBody, lonBody, imagePart)
                } else {
                    val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
                    val idBody = editingLandmark!!.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    api.updateLandmark(methodBody, idBody, titleBody, latBody, lonBody, imagePart)
                }

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                    if (parentFragmentManager.backStackEntryCount > 0) {
                        parentFragmentManager.popBackStack()
                    } else {
                         binding.etTitle.text?.clear()
                         binding.etLat.text?.clear()
                         binding.etLon.text?.clear()
                         binding.ivPreview.setImageDrawable(null)
                         pickedImageBitmap = null
                         editingLandmark = null
                         binding.btnSubmit.text = "Submit Landmark"
                    }
                } else {
                    Toast.makeText(requireContext(), "Upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resizeBitmap(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val ratio = src.width.toFloat() / src.height
        val w: Int
        val h: Int
        if (ratio > 1f) {
            w = maxW
            h = (w / ratio).toInt()
        } else {
            h = maxH
            w = (h * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

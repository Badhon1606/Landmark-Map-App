package com.example.landmark_app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.landmark_app.R
import com.example.landmark_app.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class EntryFragment : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var chooseImageButton: Button
    private lateinit var submitButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageBitmap: Bitmap? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageBitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                imageView.setImageBitmap(selectedImageBitmap)
            }
        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entry, container, false)
        titleEditText = view.findViewById(R.id.titleEditText)
        latitudeEditText = view.findViewById(R.id.latitudeEditText)
        longitudeEditText = view.findViewById(R.id.longitudeEditText)
        imageView = view.findViewById(R.id.imageView)
        chooseImageButton = view.findViewById(R.id.chooseImageButton)
        submitButton = view.findViewById(R.id.submitButton)

        chooseImageButton.setOnClickListener { pickImageFromGallery() }
        submitButton.setOnClickListener { submitLandmark() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getCurrentLocation()
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun submitLandmark() {
        val title = titleEditText.text.toString().trim()
        val latitude = latitudeEditText.text.toString().trim()
        val longitude = longitudeEditText.text.toString().trim()

        if (title.isEmpty() || latitude.isEmpty() || longitude.isEmpty() || selectedImageBitmap == null) {
            Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val resizedBitmap = resizeBitmap(selectedImageBitmap!!, 800, 600)
        val stream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = latitude.toRequestBody("text/plain".toMediaTypeOrNull())
        val lonBody = longitude.toRequestBody("text/plain".toMediaTypeOrNull())
        val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", "landmark.jpg", requestFile)

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.createLandmark(titleBody, latBody, lonBody, imagePart)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Landmark created successfully", Toast.LENGTH_SHORT).show()
                    clearForm()
                } else {
                    Toast.makeText(requireContext(), "Error creating landmark", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        titleEditText.text.clear()
        latitudeEditText.text.clear()
        longitudeEditText.text.clear()
        imageView.setImageBitmap(null)
        selectedImageBitmap = null
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                latitudeEditText.setText(it.latitude.toString())
                longitudeEditText.setText(it.longitude.toString())
            }
        }
    }

    private fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (source.width <= maxWidth && source.height <= maxHeight) {
            return source
        }
        val ratio: Float = source.width.toFloat() / source.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1) {
            newWidth = maxWidth
            newHeight = (newWidth / ratio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (newHeight * ratio).toInt()
        }
        return source.scale(newWidth, newHeight, true)
    }
}
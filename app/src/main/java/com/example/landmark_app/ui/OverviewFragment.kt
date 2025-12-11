package com.example.landmark_app.ui

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.landmark_app.R
import com.example.landmark_app.model.Landmark
import com.example.landmark_app.network.RetrofitInstance
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OverviewFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var searchView: SearchView
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var allLandmarks = mutableListOf<Landmark>()
    private var markers = mutableListOf<Marker>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                // Permission granted
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFragmentResultListener("landmarkSaved", this) { _, bundle ->
            if (bundle.getBoolean("refresh")) {
                loadLandmarks()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        Configuration.getInstance().userAgentValue = requireContext().packageName

        mapView = view.findViewById(R.id.osm_map)
        searchView = view.findViewById(R.id.searchView)

        bottomSheetView = view.findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.skipCollapsed = true

        setupMap()
        loadLandmarks()
        setupSearch()

        return view
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        mapView.controller.setZoom(8.0)
        mapView.controller.setCenter(GeoPoint(23.6850, 90.3563))

        mapView.setOnTouchListener { _, _ ->
            hideBottomSheet()
            false
        }
    }

    private fun loadLandmarks() {
        lifecycleScope.launch {
            try {
                val result = RetrofitInstance.api.getLandmarks()
                allLandmarks = result.filter { lm ->
                    !lm.title.isNullOrBlank() &&
                            !lm.latitude.isNullOrBlank() &&
                            !lm.longitude.isNullOrBlank() &&
                            !lm.image.isNullOrBlank()
                }.toMutableList()
                displayMarkers(allLandmarks)
            } catch (e: Exception) {
                Log.e("Overview", "Error loading landmarks: ${e.message}")
            }
        }
    }

    private fun displayMarkers(list: List<Landmark>) {
        mapView.overlays.removeAll { it is Marker }
        markers.clear()

        list.forEach { landmark ->
            val lat = landmark.latitude?.toDoubleOrNull()
            val lon = landmark.longitude?.toDoubleOrNull()

            if (lat == null || lon == null) {
                return@forEach
            }

            val marker = Marker(mapView)
            marker.position = GeoPoint(lat, lon)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.relatedObject = landmark
            marker.title = landmark.title
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_landmark_pin)

            marker.setOnMarkerClickListener { m, _ ->
                showBottomSheet(m.relatedObject as Landmark)
                true
            }

            markers.add(marker)
            mapView.overlays.add(marker)
        }

        if (markers.isNotEmpty()) {
            val first = markers.first()
            mapView.controller.setCenter(first.position)
        }

        mapView.invalidate()
    }


    private fun showBottomSheet(landmark: Landmark) {
        val title = bottomSheetView.findViewById<TextView>(R.id.bs_title)
        val coord = bottomSheetView.findViewById<TextView>(R.id.bs_coordinates)
        val img = bottomSheetView.findViewById<ImageView>(R.id.bs_image)
        val btnDelete = bottomSheetView.findViewById<Button>(R.id.bs_btn_delete)
        val btnEdit = bottomSheetView.findViewById<Button>(R.id.bs_btn_edit)

        title.text = landmark.title
        coord.text = "${landmark.latitude}, ${landmark.longitude}"

        Glide.with(requireContext())
            .load(landmark.fullImageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(img)

        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("id", landmark.id)
                putString("title", landmark.title)
                putString("lat", landmark.latitude)
                putString("lon", landmark.longitude)
                putString("image", landmark.image)
            }
            findNavController().navigate(R.id.entryFragment, bundle)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Landmark")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    permanentlyDeleteLandmark(landmark)
                }
                .setNegativeButton("No", null)
                .show()
        }

        bottomSheetView.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetView.visibility = View.GONE
    }

    private fun permanentlyDeleteLandmark(landmark: Landmark) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.deleteLandmark(id = landmark.id)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
                    setFragmentResult("landmarkSaved", bundleOf("refresh" to true))
                    allLandmarks.remove(landmark)
                    displayMarkers(allLandmarks)
                    hideBottomSheet()
                } else {
                    Toast.makeText(requireContext(), "Delete failed!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })
    }

    private fun filter(q: String?) {
        val filtered = if (q.isNullOrEmpty()) allLandmarks
        else allLandmarks.filter { it.title.contains(q, ignoreCase = true) }

        displayMarkers(filtered)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}

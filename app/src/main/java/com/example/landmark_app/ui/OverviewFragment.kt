package com.example.landmark_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.landmark_app.R
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OverviewFragment : Fragment() {

    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        Configuration.getInstance().userAgentValue = requireContext().packageName
        mapView = view.findViewById(R.id.osm_map)

        setupMap()
        loadLandmarks()

        return view
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(8.0)
        mapController.setCenter(GeoPoint(23.6850, 90.3563))
        mapView.invalidate()
    }

    private fun loadLandmarks() {
        lifecycleScope.launch {
            try {
                val response = com.example.landmark_app.network.RetrofitInstance.api.getLandmarks()
                if (response.isSuccessful) {
                    val landmarks = response.body() ?: emptyList()
                    displayMarkers(landmarks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayMarkers(landmarks: List<com.example.landmark_app.model.Landmark>) {
        landmarks.forEach { landmark ->
            val point = GeoPoint(landmark.latitude, landmark.longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.title = landmark.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        mapView.invalidate() // Refresh the map to show the new markers
    }
}
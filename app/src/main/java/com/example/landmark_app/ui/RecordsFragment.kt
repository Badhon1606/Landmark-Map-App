package com.example.landmark_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.landmark_app.R
import com.example.landmark_app.adapter.LandmarkAdapter
import com.example.landmark_app.network.RetrofitInstance
import com.example.landmark_app.model.Landmark
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.navigation.fragment.findNavController

class RecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LandmarkAdapter
    private var landmarkList: MutableList<Landmark> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_records, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchLandmarks()
    }

    private fun fetchLandmarks() {
        lifecycleScope.launch {
            try {
                // Now getLandmarks returns List<Landmark> directly, not Response
                val response = RetrofitInstance.api.getLandmarks()
                
                landmarkList = response.toMutableList()
                adapter = LandmarkAdapter(landmarkList)
                recyclerView.adapter = adapter

                attachSwipeGestures()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attachSwipeGestures() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val landmark = landmarkList[position]

                when (direction) {

                    // 👉 Swipe Right for Edit
                    ItemTouchHelper.RIGHT -> {
                        adapter.notifyItemChanged(position)
                        val bundle = Bundle().apply {
                            putInt("id", landmark.id)
                            putString("title", landmark.title)
                            // Latitude and longitude in Landmark are Strings
                            putString("lat", landmark.latitude)
                            putString("lon", landmark.longitude)
                            putString("image", landmark.image)
                        }
                        findNavController().navigate(R.id.entryFragment, bundle)
                    }

                    // 👈 Swipe Left for Delete
                    ItemTouchHelper.LEFT -> {
                        deleteLandmark(position, landmark)
                    }
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun deleteLandmark(position: Int, landmark: Landmark) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.deleteLandmark(id = landmark.id)

                if (response.isSuccessful) {
                    landmarkList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                    adapter.notifyItemChanged(position)
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                adapter.notifyItemChanged(position)
            }
        }
    }
}
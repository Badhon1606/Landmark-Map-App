package com.example.landmark_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.landmark_app.R
import com.example.landmark_app.adapter.LandmarkAdapter
import com.example.landmark_app.model.Landmark
import com.example.landmark_app.network.RetrofitInstance
import kotlinx.coroutines.launch

class RecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LandmarkAdapter
    private var landmarkList: MutableList<Landmark> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener("landmarkSaved") { _, bundle ->
            if (bundle.getBoolean("refresh")) {
                fetchLandmarks()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                val landmarks = RetrofitInstance.api.getLandmarks()
                landmarkList = landmarks.filter { lm ->
                    !lm.title.isNullOrBlank()
                            && !lm.latitude.isNullOrBlank()
                            && !lm.longitude.isNullOrBlank()
                            && !lm.image.isNullOrBlank()
                }.toMutableList()

                adapter = LandmarkAdapter(landmarkList)
                recyclerView.adapter = adapter

                attachSwipeGestures()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attachSwipeGestures() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                when (direction) {

                    // 👉 Swipe RIGHT → Edit
                    ItemTouchHelper.RIGHT -> {
                        adapter.notifyItemChanged(position)
                        val landmark = adapter.getLandmarkAt(position)
                        val bundle = Bundle().apply {
                            putInt("id", landmark.id)
                            putString("title", landmark.title)
                            putString("lat", landmark.latitude)
                            putString("lon", landmark.longitude)
                            putString("image", landmark.image)
                        }
                        findNavController().navigate(R.id.entryFragment, bundle)
                    }

                    // 👈 Swipe LEFT → Delete
                    ItemTouchHelper.LEFT -> {
                        deleteLandmark(position)
                    }
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun deleteLandmark(position: Int) {
        val landmark = adapter.getLandmarkAt(position)
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.deleteLandmark(id = landmark.id)

                if (response.isSuccessful) {
                    landmarkList.remove(landmark)
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

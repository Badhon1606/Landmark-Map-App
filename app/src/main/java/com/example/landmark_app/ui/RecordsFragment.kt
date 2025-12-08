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
import kotlinx.coroutines.launch

class RecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LandmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_records, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchLandmarks()
    }

    private fun fetchLandmarks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getLandmarks()
                if (response.isSuccessful && response.body() != null) {
                    adapter = LandmarkAdapter(response.body()!!)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(context, "Error fetching landmarks", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package com.example.landmark_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.landmark_app.R
import com.example.landmark_app.model.Landmark

class LandmarkAdapter(private val landmarks: List<Landmark>) :
    RecyclerView.Adapter<LandmarkAdapter.LandmarkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandmarkViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_landmark, parent, false)

        return LandmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LandmarkViewHolder, position: Int) {
        holder.bind(landmarks[position])
    }

    override fun getItemCount(): Int = landmarks.size

    class LandmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(landmark: Landmark) {
            titleTextView.text = landmark.title
            locationTextView.text = "Lat: ${landmark.latitude}, Lon: ${landmark.longitude}"

            val fullUrl =
                "https://labs.anontech.info/cse489/t3/images/${landmark.imageUrl}"

            Glide.with(itemView.context)
                .load(fullUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        }
    }
}

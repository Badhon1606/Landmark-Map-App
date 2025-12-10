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

class LandmarkAdapter(private val landmarks: MutableList<Landmark>) :
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

    fun removeAt(position: Int) {
        if (position >= 0 && position < landmarks.size) {
            landmarks.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getLandmarkAt(position: Int): Landmark {
        return landmarks[position]
    }

    class LandmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(landmark: Landmark) {
            titleTextView.text = landmark.title
            locationTextView.text = "Lat: ${landmark.latitude}, Lon: ${landmark.longitude}"

            Glide.with(itemView.context)
                .load(landmark.fullImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert) // Changed from ic_menu_report_image
                .into(imageView)
        }
    }
}
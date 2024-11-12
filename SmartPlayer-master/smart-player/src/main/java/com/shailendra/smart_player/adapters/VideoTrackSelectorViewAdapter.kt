package com.shailendra.smart_player.adapters

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.shailendra.smart_player.R
import com.shailendra.smart_player.data.VideoTracksInfo
import com.shailendra.smart_player.interfaces.VideoTrackSelectionNotifier


class VideoTrackSelectorViewAdapter(
    private val context: Context,
    private val supportedVideoTracksAvailable: ArrayList<VideoTracksInfo>,
    private val videoTrackSelectionNotifier: VideoTrackSelectionNotifier,
    private var selectedVideoTrackPosition: Int
) :
    RecyclerView.Adapter<VideoTrackSelectorViewAdapter.ViewHolder>(),
    VideoTrackSelectionNotifier by videoTrackSelectionNotifier {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.video_track_selection_item_view, parent, false)
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(supportedVideoTracksAvailable[position])

        holder.videoTrackQuality.setOnClickListener {
            holder.videoTrackQuality.setBackgroundResource(R.drawable.bg_selection_highlight)
            holder.videoTrackQuality.setTextColor(context.resources.getColor(R.color.selected_text_highlight_color, null))
            videoTrackSelectionNotifier.notifySelectedVideoTrackPosition(position)
            selectedVideoTrackPosition = position
            notifyDataSetChanged()
        }

        if (position == selectedVideoTrackPosition) {
            holder.videoTrackQuality.setBackgroundResource(R.drawable.bg_selection_highlight)
            holder.videoTrackQuality.setTextColor(context.resources.getColor(R.color.selected_text_highlight_color, null))
        }
        else {
            holder.videoTrackQuality.setBackgroundColor(Color.TRANSPARENT)
            holder.videoTrackQuality.setTextColor(context.resources.getColor(R.color.exo_white, null))
        }
    }

    override fun getItemCount(): Int {
        return supportedVideoTracksAvailable.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val videoTrackQuality: TextView = itemView.findViewById(R.id.video_track_quality)

        fun bind(videoTracksInfo: VideoTracksInfo) {
            if (videoTracksInfo.videoTrackQuality != "Auto")
                videoTrackQuality.text = "${videoTracksInfo.videoTrackQuality}p"
            else
                videoTrackQuality.text = "${videoTracksInfo.videoTrackQuality}"
        }
    }

}
package com.example.lab4

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AudioTrackAdapter(
    private val context: Context,
    private val tracks: List<Uri>,
    private val onClick: (Int) -> Unit,
    private val getCurrentIndex: () -> Int
) : RecyclerView.Adapter<AudioTrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivItemCover)
        val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvItemArtist)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val uri = tracks[position]
        val isUrl = uri.scheme == "http" || uri.scheme == "https"

        if (isUrl) {
            val raw = uri.lastPathSegment ?: uri.toString()
            val clean = raw.substringBefore('?').substringBeforeLast('.')
            holder.tvTitle.text = clean
            holder.tvArtist.text = "URL track"
            holder.ivCover.setImageResource(R.drawable.ic_music_placeholder)
        } else {
            val retriever = MediaMetadataRetriever()
            var title: String? = null
            var artist: String? = null
            var art: ByteArray? = null

            try {
                retriever.setDataSource(context, uri)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                art = retriever.embeddedPicture
            } catch (_: Exception) {}

            if (title.isNullOrBlank()) {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex("_display_name")
                    if (nameIndex >= 0 && it.moveToFirst()) {
                        title = it.getString(nameIndex)
                    }
                }
            }

            if (title.isNullOrBlank()) title = "Unknown Title"
            if (artist.isNullOrBlank()) artist = "Unknown Artist"

            holder.tvTitle.text = title
            holder.tvArtist.text = artist

            if (art != null) {
                val bmp = BitmapFactory.decodeByteArray(art, 0, art.size)
                holder.ivCover.setImageBitmap(bmp)
            } else {
                holder.ivCover.setImageResource(R.drawable.ic_music_placeholder)
            }
        }

        if (position == getCurrentIndex()) {
            holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_700))
        } else {
            holder.root.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }

        holder.itemView.setOnClickListener {
            onClick(position)
        }
    }

    override fun getItemCount(): Int = tracks.size
}

package com.evil19_96.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelAdapter(
    private val onPlay: (Channel) -> Unit,
    private val onFavorite: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.Holder>() {
    private val items = mutableListOf<Channel>()
    private val favorites = mutableSetOf<String>()
    fun submit(list: List<Channel>, favoriteUrls: Set<String> = emptySet()) {
        items.clear(); items.addAll(list); favorites.clear(); favorites.addAll(favoriteUrls); notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_channel, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.group.text = item.group
        holder.favorite.text = if (favorites.contains(item.url)) "★" else "☆"
        holder.itemView.setOnClickListener { onPlay(item) }
        holder.favorite.setOnClickListener { onFavorite(item) }
    }
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.channelName)
        val group: TextView = view.findViewById(R.id.channelGroup)
        val favorite: Button = view.findViewById(R.id.favoriteButton)
    }
}

package com.gnoemes.shikimori.presentation.view.anime.adapter

import androidx.recyclerview.widget.DiffUtil
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.anime.presentation.AnimeHeadItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.presentation.view.common.adapter.DetailsContentAdapterDelegate
import com.gnoemes.shikimori.presentation.view.common.adapter.DetailsDescriptionAdapterDelegate
import com.gnoemes.shikimori.presentation.view.common.adapter.DetailsMoreAdapterDelegate
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter

class AnimeAdapter(
        imageLoader: ImageLoader,
        detailsCallback: (DetailsAction) -> Unit,
        navigationCallback: (Type, Long) -> Unit,
        settings: SettingsSource
) : ListDelegationAdapter<MutableList<Any>>() {

    init {
        with(delegatesManager) {
            addDelegate(AnimeHeadAdapterDelegate(imageLoader, detailsCallback, settings))
            addDelegate(DetailsMoreAdapterDelegate(detailsCallback))
            addDelegate(DetailsDescriptionAdapterDelegate())
            addDelegate(DetailsContentAdapterDelegate(imageLoader, settings, navigationCallback, detailsCallback))
        }

        setItems(mutableListOf())
    }

    fun bindItems(newItems: List<Any>) {
        val callback = DiffCallback(items, newItems)
        items.clear()
        items.addAll(newItems)
        DiffUtil.calculateDiff(callback)
                .dispatchUpdatesTo(this)
    }

    fun updateCharacters(it: Any) {
        updateItemWithContentType(it, DetailsContentType.CHARACTERS)
    }

    fun updateSimilar(it: Any) {
        updateItemWithContentType(it, DetailsContentType.SIMILAR)
    }

    fun updateRelated(it: Any) {
        updateItemWithContentType(it, DetailsContentType.RELATED)
    }

    fun updateHead(it: Any) {
        val index = items.indexOfFirst { it is AnimeHeadItem }
        items[index] = it
        notifyItemChanged(index)
    }

    private fun updateItemWithContentType(it: Any, type: DetailsContentType) {
        if (it is DetailsContentItem.Empty) {
            val index = items.indexOfFirst { it is DetailsContentItem.Loading && it.contentType == type }
            items.removeAt(index)
            notifyItemRemoved(index)
        } else {
            val index = items.indexOfFirst { it is DetailsContentItem.Loading && it.contentType == type }
            items[index] = it
            notifyItemChanged(index)
        }
    }


    private inner class DiffCallback(
            private val oldItems: List<Any>,
            private val newItems: List<Any>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldItems[oldItemPosition] == newItems[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
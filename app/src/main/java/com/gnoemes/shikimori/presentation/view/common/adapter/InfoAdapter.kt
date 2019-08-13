package com.gnoemes.shikimori.presentation.view.common.adapter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.InfoClickableItem
import com.gnoemes.shikimori.entity.common.presentation.InfoItem

class InfoAdapter(
        navigationCallback: (Type, Long) -> Unit
) : BaseAdapter<Any>() {

    init {
        delegatesManager.apply {
            addDelegate(InfoAdapterDelegate())
            addDelegate(InfoClickableAdapterDelegate(navigationCallback))
        }
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is InfoItem && newItem is InfoItem -> oldItem.category == newItem.category
        oldItem is InfoClickableItem && newItem is InfoClickableItem -> oldItem.id == newItem.id && oldItem.type == newItem.type
        else -> false
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is InfoItem && newItem is InfoItem -> oldItem == newItem
        oldItem is InfoClickableItem && newItem is InfoClickableItem -> oldItem == newItem
        else -> false
    }
}
package com.github.sample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kamsyview.extensions.loadAvatar
import com.github.sample.databinding.ItemSampleListBinding

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
class SampleAdapter : ListAdapter<User, SampleAdapter.ViewHolder>(User.DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemSampleListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SampleAdapter.ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ViewHolder(private val binding: ItemSampleListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) = with(binding) {
            avatar.loadAvatar(user.avatar, user.name)
            name.text = user.name
        }
    }
}

package com.github.sample

import androidx.recyclerview.widget.DiffUtil

/**
 * @author Mbuodile Obiosio
 * https://linktr.ee/mbobiosio
 */
data class User(
    val name: String,
    val avatar: String,
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(
                oldItem: User,
                newItem: User,
            ): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(
                oldItem: User,
                newItem: User,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}

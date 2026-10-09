package com.minecraftmc22.expenses.home.domain

import com.minecraftmc22.expenses.data.model.Tag

class SortTagsUseCase {
    operator fun invoke(tags: List<Tag>): List<Tag> {
        return tags.sortedBy { it.name }
    }
}
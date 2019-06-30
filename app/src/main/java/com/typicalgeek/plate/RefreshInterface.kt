@file:JvmName("RefreshInterface")

package com.typicalgeek.plate

internal interface RefreshInterface {
    fun refreshAll(shouldRefresh: Boolean = false)
}
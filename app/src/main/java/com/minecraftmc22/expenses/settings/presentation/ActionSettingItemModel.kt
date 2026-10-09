package com.minecraftmc22.expenses.settings.presentation

open class ActionSettingItemModel(val title: String):
    SettingItemModel {

    var click: (() -> Unit)? = null
}
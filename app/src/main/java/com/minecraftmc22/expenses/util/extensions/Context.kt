package com.minecraftmc22.expenses.util.extensions

import android.content.Context
import com.minecraftmc22.expenses.Application

val Context.application: Application
    get() = applicationContext as Application
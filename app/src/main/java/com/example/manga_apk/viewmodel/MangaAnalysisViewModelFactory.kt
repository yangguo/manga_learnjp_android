package com.example.manga_apk.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MangaAnalysisViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaAnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaAnalysisViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.example.brave_sailors.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.brave_sailors.data.local.database.UserDao

class ProfileViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown class \"ViewModel\".")
    }
}
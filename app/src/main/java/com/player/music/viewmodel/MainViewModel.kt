// viewmodel/MainViewModel.kt

package com.player.music.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.repository.UserRepository
import com.player.music.local.DataStoreManager
import com.player.music.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val _isTokenValid = MutableStateFlow<Boolean?>(null)

    // 用于在 LaunchPage 中监听验证结果
    val isTokenValid: StateFlow<Boolean?> = _isTokenValid.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // 1. 获取缓存的用户数据
                val cachedUser = dataStoreManager.getUser().firstOrNull()
                _currentUser.value = cachedUser

                // 2. 获取缓存的 token
                var cachedToken = dataStoreManager.getToken().firstOrNull()

                _token.value = cachedToken

            } catch (e: Exception) {
                // 静默处理异常
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 验证 token 有效性（调用接口验证）
     */
    suspend fun validateToken(): Boolean {
        return try {
            // 从 StateFlow 获取当前 token
            val currentToken = _token.value

            if (currentToken.isNullOrBlank()) {
                _isTokenValid.value = false
                return false
            }

            // 调用接口验证 token
            val result = userRepository.getUserData()

            if (result.isSuccess) {
                val user = result.getOrNull()?.first
                val newToken = result.getOrNull()?.second

                // 更新用户和 token
                user?.let { updateUser(it) }
                newToken?.let { updateToken(it) }

                _isTokenValid.value = true
                true
            } else {
                _isTokenValid.value = false
                false
            }

        } catch (e: Exception) {
            _isTokenValid.value = false
            false
        }
    }

    /**
     * 本地检查 token 是否存在（不验证有效性）
     */
    fun hasTokenLocally(): Boolean {
        return !_token.value.isNullOrBlank()
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            dataStoreManager.saveUser(user)
            _currentUser.value = user
        }
    }

    fun updateToken(token: String) {
        viewModelScope.launch {
            dataStoreManager.saveToken(token)
            _token.value = token
        }
    }

    fun clearUserData() {
        viewModelScope.launch {
            dataStoreManager.clearAll()
            _currentUser.value = null
            _token.value = null
        }
    }

}
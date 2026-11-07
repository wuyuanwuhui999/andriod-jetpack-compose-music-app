package com.player.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.music.local.DataStoreManager
import com.player.music.model.User
import com.player.music.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 用户信息 ViewModel：头像上传、资料修改、退出登录
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun showLogoutDialog() {
        _showLogoutDialog.value = true
    }

    fun hideLogoutDialog() {
        _showLogoutDialog.value = false
    }

    /**
     * 退出登录，清空本地缓存
     */
    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    /**
     * 更新用户头像
     * @param file 头像文件
     */
    fun updateAvatar(file: File) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val result = userRepository.updateAvatar(file)
                if (result.isSuccess) {
                    _updateResult.value = UpdateResult.Success("头像更新成功")
                } else {
                    _updateResult.value = UpdateResult.Error(result.exceptionOrNull()?.message ?: "头像更新失败")
                }
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(e.message ?: "头像更新失败")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    /**
     * 更新用户信息
     * @param user 更新后的用户对象
     */
    fun updateUserInfo(user: User) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val result = userRepository.updateUser(user)
                if (result.isSuccess) {
                    // 同步落到本地缓存，其他地方（如用户信息页）返回时可读到最新数据
                    runCatching { dataStoreManager.saveUser(user) }
                    _updateResult.value = UpdateResult.Success("用户信息更新成功")
                } else {
                    _updateResult.value = UpdateResult.Error(result.exceptionOrNull()?.message ?: "用户信息更新失败")
                }
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(e.message ?: "用户信息更新失败")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}

/**
 * 更新结果密封类
 */
sealed class UpdateResult {
    data class Success(val message: String) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

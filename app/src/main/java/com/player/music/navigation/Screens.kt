package com.player.music.navigation

/**
 * 全局路由表
 */
sealed class Screens(val route: String) {
    object Launch : Screens("launch")
    object Login : Screens("login")
    object Home : Screens("home")
    object User : Screens("user")
    object UpdatePassword : Screens("update_password")
    object ForgetPassword : Screens("forget_password")
    object ResetPassword : Screens("reset_password")
    object Register : Screens("register")
}

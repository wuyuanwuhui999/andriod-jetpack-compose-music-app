package com.player.music.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.player.music.ui.page.ForgetPasswordPage
import com.player.music.ui.page.HomePage
import com.player.music.ui.page.LaunchPage
import com.player.music.ui.page.LoginPage
import com.player.music.ui.page.RegisterPage
import com.player.music.ui.page.ResetPasswordPage
import com.player.music.ui.page.UpdatePasswordPage
import com.player.music.ui.page.UserPage

/**
 * 导航图：启动页 -> 登录页 -> 首页
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.Launch.route
    ) {
        composable(Screens.Launch.route) {
            LaunchPage(navController = navController)
        }
        composable(Screens.Login.route) {
            LoginPage(navController = navController)
        }
        composable(Screens.Home.route) {
            HomePage(navController = navController)
        }
        composable(Screens.User.route) {
            UserPage(navController = navController)
        }
        composable(Screens.UpdatePassword.route) {
            UpdatePasswordPage(navController = navController)
        }
        composable(Screens.ForgetPassword.route) {
            ForgetPasswordPage(navController = navController)
        }
        composable(
            route = "${Screens.ResetPassword.route}?email={email}",
            arguments = listOf(navArgument("email") { defaultValue = ""; type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordPage(navController = navController, email = email)
        }
        composable(Screens.Register.route) {
            RegisterPage(navController = navController)
        }
    }
}

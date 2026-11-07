package com.player.music.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.player.music.ui.page.ForgetPasswordPage
import com.player.music.ui.page.ChatPage
import com.player.music.ui.page.EditPage
import com.player.music.ui.page.LaunchPage
import com.player.music.ui.page.LoginPage
import com.player.music.ui.page.MusicIndexPage
import com.player.music.ui.page.RegisterPage
import com.player.music.ui.page.ResetPasswordPage
import com.player.music.ui.page.UpdatePasswordPage
import com.player.music.ui.page.MusicPlayerPage
import com.player.music.ui.page.MusicAuthorCategoryPage
import com.player.music.ui.page.MusicAuthorPage
import com.player.music.ui.page.MusicCategoryPage
import com.player.music.ui.page.MusicClassifyListPage
import com.player.music.ui.page.MusicFavoriteListPage
import com.player.music.ui.page.MusicLyricPage
import com.player.music.ui.page.MusicPickPage
import com.player.music.ui.page.MusicSearchPage
import com.player.music.ui.page.RecordMusicPage
import com.player.music.ui.page.MusicSharePage
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
        composable(Screens.MusicIndex.route) {
            MusicIndexPage(navController = navController)
        }
        composable(Screens.User.route) {
            UserPage(navController = navController)
        }
        composable(Screens.MusicPlayer.route) {
            MusicPlayerPage(navController = navController)
        }
        composable(Screens.MusicLyric.route) {
            MusicLyricPage(navController = navController)
        }
        composable(Screens.Chat.route) {
            ChatPage(navController = navController)
        }
        composable(Screens.MusicShare.route) {
            MusicSharePage(navController = navController)
        }
        composable(Screens.MusicPick.route) {
            MusicPickPage(navController = navController)
        }
        composable(Screens.MusicAuthorCategory.route) {
            MusicAuthorCategoryPage(navController = navController)
        }
        composable(Screens.MusicCategory.route) {
            MusicCategoryPage(navController = navController)
        }
        composable(Screens.MusicRecord.route) {
            RecordMusicPage(navController = navController)
        }
        composable(
            route = Screens.Edit.route,
            arguments = listOf(
                navArgument("title") { defaultValue = ""; type = NavType.StringType },
                navArgument("field") { defaultValue = ""; type = NavType.StringType },
                navArgument("value") { defaultValue = ""; type = NavType.StringType },
                navArgument("allowEmpty") { defaultValue = "true"; type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditPage(
                navController = navController,
                title = backStackEntry.arguments?.getString("title") ?: "",
                field = backStackEntry.arguments?.getString("field") ?: "",
                initialValue = backStackEntry.arguments?.getString("value") ?: "",
                allowEmpty = backStackEntry.arguments?.getString("allowEmpty") != "false"
            )
        }
        composable(
            route = Screens.MusicFavoriteList.route,
            arguments = listOf(
                navArgument("directoryId") { defaultValue = "0"; type = NavType.StringType },
                navArgument("directoryName") { defaultValue = ""; type = NavType.StringType },
                navArgument("cover") { defaultValue = ""; type = NavType.StringType },
                navArgument("total") { defaultValue = "0"; type = NavType.StringType }
            )
        ) {
            MusicFavoriteListPage(navController = navController)
        }
        composable(
            route = Screens.MusicClassifyList.route,
            arguments = listOf(
                navArgument("classifyId") { defaultValue = "0"; type = NavType.StringType },
                navArgument("classifyName") { defaultValue = "分类歌曲"; type = NavType.StringType }
            )
        ) {
            MusicClassifyListPage(navController = navController)
        }
        composable(
            route = Screens.MusicSearch.route,
            arguments = listOf(
                navArgument("keyword") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            MusicSearchPage(
                navController = navController,
                initialKeyword = backStackEntry.arguments?.getString("keyword") ?: ""
            )
        }
        composable(
            route = Screens.MusicAuthor.route,
            arguments = listOf(
                navArgument("authorId") { defaultValue = 0L; type = NavType.LongType },
                navArgument("authorName") { defaultValue = ""; type = NavType.StringType },
                navArgument("avatar") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            MusicAuthorPage(
                navController = navController,
                authorId = backStackEntry.arguments?.getLong("authorId") ?: 0L,
                authorName = backStackEntry.arguments?.getString("authorName") ?: "",
                avatar = backStackEntry.arguments?.getString("avatar") ?: ""
            )
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

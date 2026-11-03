package com.player.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.player.music.navigation.AppNavGraph
import com.player.music.ui.theme.MusicTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity，承载 Compose 页面
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    MusicTheme {
        val navController = rememberNavController()
        AppNavGraph(navController = navController)
    }
}

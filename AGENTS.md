# AGENTS.md — 给 AI 智能体的项目说明书

> 目标是让一个没看过这个仓库的 AI 编码助手（Codex / Claude Code / Cursor / Gemini CLI 等）**在 5 分钟内**知道：这是什么项目、代码怎么分层、页面在哪里、接口怎么调、改一个功能要动哪些文件、怎么验证、有哪些坑。
>
> 项目：**Android 音乐 App（Jetpack Compose）** · 包名 `com.player.music` · 应用名「Music」· 版本 1.0
> 同一位作者的平行实现：Flutter 版 `flutter-music-app-ui`、uni-app Vue3 版 `uniapp-vite-vue3-ts-music-app-ui`，功能基本一一对应，**移植新页面时以它们为准**。

---

## 1. 30 秒速览

| 项 | 值 |
| --- | --- |
| 类型 | 单 Activity + Jetpack Compose 原生 Android App |
| 语言/构建 | Kotlin 1.9.23 / AGP 8.5.0 / Gradle Kotlin DSL |
| 架构 | MVVM：Compose UI → ViewModel(StateFlow) → Repository → Retrofit ApiService |
| UI 库 | Compose BOM 2023.10.01 + **Material3 1.1.x**（版本坑很多，见 §7） |
| DI | Hilt 2.51（`MusicApplication` 带 `@HiltAndroidApp`，`MainActivity` 带 `@AndroidEntryPoint`） |
| 网络 | Retrofit 2.11 + OkHttp 4.12 + Gson |
| 图片 | Coil 2.5.0 |
| 本地存储 | DataStore Preferences（token / 用户信息 / 搜索历史 / 循环模式） |
| 音频播放 | **系统 MediaPlayer**（`player/MusicPlayerManager.kt`，单例，无 ExoPlayer 依赖） |
| SDK | compileSdk/targetSdk 35，minSdk 24 |
| 后端 | 本机 FastAPI 多模块网关，形如 `http://<局域网IP>:4000/service/{music,social,circle,user,chat}/...` |
| 测试账号 | 账号「吴时吴刻」/ 密码 `123456` |
| 远端仓库 | gitee `wuyuanwuhui99/andriod-jetpack-compose-music-app` · github `wuyuanwuhui999/andriod-jetpack-compose-music-app`（分支 main） |

---

## 2. 目录结构

```
andriod-jetpack-compose-music-app/
├─ app/src/main/java/com/player/music/
│  ├─ MainActivity.kt              # 唯一 Activity，setContent { MusicTheme { NavGraph } }
│  ├─ MusicApplication.kt          # @HiltAndroidApp
│  ├─ config/
│  │  ├─ Config.kt                 # BASE_URL（本机局域网 IP，属本地环境改动，提交时排除！）
│  │  └─ ChatConfig.kt             # AI 聊天：COMPANY_ID / WS 路径 / 分页大小
│  ├─ navigation/
│  │  ├─ Screens.kt                # 全部路由常量 + 带参路由的 createRoute()
│  │  └─ NavGraph.kt               # NavHost：每个路由注册对应 Composable + navArgument
│  ├─ ui/
│  │  ├─ page/                     # 所有页面（一个文件一个页面）
│  │  ├─ components/               # 复用组件（MusicCover / SongListCard / Avatar / 各种 Dialog）
│  │  └─ theme/                    # Color.kt / Dimens.kt / Theme.kt / Type.kt
│  ├─ viewmodel/                   # 每个页面一个 @HiltViewModel（StateFlow 驱动 UI）
│  ├─ repository/                  # MusicRepository / CircleRepository / UserRepository / ChatRepository
│  ├─ network/                     # ApiService.kt（全部接口集中在这一个文件）+ NetworkInterceptor
│  ├─ model/                       # Gson 数据类 + ApiResponse / XxxListResult 分页结果
│  ├─ player/MusicPlayerManager.kt # 播放器单例：playList / togglePlay / next / previous / seekTo / 循环模式
│  ├─ local/                       # DataStoreManager + PreferenceKeys
│  ├─ utils/                       # LyricParser（LRC 解析）、ChatContentParser（think/music 拆解）、CommonUtils 等
│  └─ di/AppModule.kt              # Hilt：OkHttp / Retrofit / ApiService / Repository 的 Provide
├─ gradle/libs.versions.toml       # 依赖版本集中管理
├─ README.md                       # 面向人的说明 + 页面进度表（新增页面记得补一行）
└─ AGENTS.md                       # 本文件
```

**改动新功能时最常动的 6 个文件**：`ui/page/XxxPage.kt`（新）、`viewmodel/XxxViewModel.kt`（新）、`navigation/Screens.kt`、`navigation/NavGraph.kt`、`network/ApiService.kt`、`README.md`（进度表）。

---

## 3. 数据流（照抄这个模式写新页面）

```
Compose Page ──collectAsState()──> ViewModel StateFlow ──viewModelScope──> Repository
      │                                    ▲                                  │
      └──事件(点击/输入)──> viewModel.fun()──┘                    suspend → ApiService(Retrofit)
                                                                        │
                                             ApiResponse<T>{ data, total, status, msg, token }
```

约定：

- `ApiResponse.status == "SUCCESS"` 视为成功，**分页总数在 envelope 的 `total` 字段**，不在 `data` 里。
- Repository 统一返回 `Result<T>`（`Result.success(...)` / `Result.failure(Exception(msg))`），ViewModel 里判断 `isSuccess`。
- 页面只做展示与事件转发，**不发网络请求**；弹 Toast 用 `_toastMessage: StateFlow<String?>` + 页面里 `LaunchedEffect(toastMessage)` 消费。
- 点赞/收藏这类按钮：**先本地翻转 UI，再调接口，失败回滚 + Toast**，不要等接口回来才刷新。
- 播放走 `MusicPlayerManager`（单例，`@Inject constructor()` 直接注入 ViewModel）：
  `player.playList(list, index)`（整页作为播放列表）、`player.togglePlay()`、`player.next()`、`player.seekTo(ms)`；
  `player.currentMusic / isPlaying / positionMs / durationMs / loopMode` 都是 StateFlow。

---

## 4. 路由与页面清单

**路由三件套**（缺一不可，否则页面不可达）：

1. `Screens.kt` 加 `object Xxx : Screens("xxx")`，带参用 `Screens("xxx?a={a}")` + `fun createRoute(...)`（参数 `Uri.encode`）。
2. `NavGraph.kt` 加 `composable(Screens.Xxx.route) { XxxPage(navController = navController) }`；带参要 `arguments = listOf(navArgument("a") { defaultValue = ""; type = NavType.StringType })`，并在 ViewModel 里用 `SavedStateHandle` 读参数。
3. 入口处 `navController.navigate(Screens.Xxx.createRoute(...))`。

| 路由 | 页面文件 | ViewModel | 入口 / 说明 |
| --- | --- | --- | --- |
| `launch` | LaunchPage.kt | MainViewModel | 启动页：校验本地 token |
| `login` | LoginPage.kt | LoginViewModel | 账号密码 / 邮箱验证码登录 |
| `register` | RegisterPage.kt | RegisterViewModel | 注册 |
| `forget_password` | ForgetPasswordPage.kt | ForgetPasswordViewModel | 忘记密码（发验证码） |
| `reset_password` | ResetPasswordPage.kt | ResetPasswordViewModel | 重置密码 |
| `music_index` | MusicIndexPage.kt | MusicIndexViewModel | **主容器**：底部四 Tab + 中间悬浮迷你播放器（播放时封面旋转） |
| ├ Tab0 `home` | MusicHomePage.kt | MusicIndexViewModel | 搜索栏（头像+关键字+AI）、四个快捷入口、分类模块、上拉加载 |
| ├ Tab1 推荐 | MusicRecommendPage.kt | MusicRecommendViewModel | 榜单列表（前三名奖牌）、点赞 |
| ├ Tab2 音乐圈 | MusicCirclePage.kt | MusicCircleViewModel | 动态列表、点赞、评论与二级回复、右下角「+」发布 |
| └ Tab3 我的 | MusicUserPage.kt | MusicUserViewModel | 用户卡、收藏夹、我喜欢的歌曲、我关注的歌手、播放记录（可折叠/刷新） |
| `music_player` | MusicPlayerPage.kt | MusicPlayerViewModel | 模糊封面背景、旋转唱片、歌词滚动、喜欢/分享/评论/收藏夹、进度拖动、循环模式、播放列表弹层 |
| `music_lyric` | MusicLyricPage.kt | MusicPlayerViewModel | 全屏歌词：当前行高亮、自动居中滚动、点某句跳转进度 |
| `music_share` | MusicSharePage.kt | MusicShareViewModel | 分享到音乐圈（文字 + 歌曲 + 可见范围） |
| `music_pick` | MusicPickPage.kt | MusicPickViewModel | 分享时搜索并选一首歌 |
| `music_search?keyword=` | MusicSearchPage.kt | MusicSearchViewModel | 搜索：历史标签（DataStore）、结果分页、点赞 |
| `music_author_category` | MusicAuthorCategoryPage.kt | MusicAuthorCategoryViewModel | 歌手分类网格 + 歌手列表 |
| `music_author?...` | MusicAuthorPage.kt | MusicAuthorViewModel | 歌手详情 + 关注 + 该歌手歌曲 |
| `music_category` | MusicCategoryPage.kt | MusicCategoryViewModel | 歌曲分类（3 列网格，可展开/收起） |
| `music_classify?...` | MusicClassifyListPage.kt | MusicClassifyListViewModel | 某分类歌曲列表（首页模块「更多」） |
| `music_favorite?...` | MusicFavoriteListPage.kt | MusicFavoriteListViewModel | 收藏夹内歌曲列表 |
| `music_record` | RecordMusicPage.kt | RecordMusicViewModel | 最近听过的歌曲（含「听过 N 次」） |
| `chat` | ChatPage.kt | ChatViewModel | AI 聊天：WS 流式、深度思考、模型切换、会话记录抽屉、`<music>` 歌单卡片 |
| `user` | UserPage.kt | UserViewModel | 用户信息（头像上传、字段编辑、退出登录） |
| `edit?...` | EditPage.kt | MainViewModel + UserViewModel | 通用单字段编辑页（昵称/电话/邮箱/地区/签名） |
| `update_password` | UpdatePasswordPage.kt | UpdatePasswordViewModel | 修改密码 |

公共组件：`MusicCover`（封面，缺省图可传 `fallbackRes`）、`SongListCard`（歌单行：封面+歌手-歌名+播放+点赞+更多，多个页面共用）、`Avatar`、`InputDialog`、`DatePickerDialog`、`CustomAlertDialog`。

---

## 5. 后端接口

- 网关地址：`Config.BASE_URL`（默认 `http://192.168.5.62:4000`，**随局域网变化**，改这里只改本地，不提交）。
- 模块：`/service/music/*`、`/service/social/*`（评论/点赞）、`/service/circle/*`（音乐圈）、`/service/user/*`（登录/用户）、`/service/chat/*`（AI）。
- 鉴权：除 `getModelList` 等少数接口外，都要 `Authorization: Bearer <token>`。`network/NetworkInterceptor.kt` 会从 `DataStoreManager` 自动补这个头；项目里**没有全局 401 拦截**，token 失效表现为接口错误，由各页面自己提示。
- 已封装的接口全在 `network/ApiService.kt`（约 40 个），改接口只动这一个文件 + 对应 Repository。
- 分页参数统一 `pageNum`（从 1 开始）/ `pageSize`；返回 `ApiResponse<T>{ data, total, status, msg, token }`。
- **AI 聊天走 WebSocket**：`ws://<host>/service/chat/ws/chat?token=Bearer%20<jwt>`（token 必须带 `Bearer ` 前缀并 URL 编码）。首帧发 JSON：
  `{ modelId, chatId, prompt, showThink, language, companyId, type }`（`companyId` 后端必填，取 `ChatConfig.COMPANY_ID`）。
  服务端逐块返回文本，结束发 `[completed]`；思考内容包在 `<think>...</think>`，歌单包在 `<music>[...]</music>`（JSON 数组）。

---

## 6. 代码约定（务必遵守，否则会跟现有代码风格打架）

- 尺寸/颜色**只从 `Dimens.*` / `Color.*` 取**（`middleGap`、`smallGap`、`bigGap`、`moduleBorderRadius`、`bigBorderRadius`、`btnHeight`、`inputHeight`、`normalFontSize`、`bigFontSize`、`middleIconSize`、`listIconSize` 等），**不要硬编码 dp/sp**。
- 页面根布局：`Modifier.fillMaxSize().background(Color.PageBackground)`；**被 push 出来的页面**顶部还要 `.statusBarsPadding()`（Tab 页由 Scaffold 处理），列表底部留 `navigationBarsPadding()`。
- 卡片统一样式：白色 + `RoundedCornerShape(Dimens.moduleBorderRadius)`，左右外边距 `Dimens.middleGap`。
- 列表用 `LazyColumn`；上拉加载不要用 footer 里的 `LaunchedEffect`，用 `LazyListState` + `derivedStateOf` 检测滚到底（否则永远只加载第一页）。
- 每个页面一个 `@HiltViewModel`，状态用 `MutableStateFlow` + `asStateFlow()`，协程只在 `viewModelScope` 里。
- 注释写中文；提交信息写中文，说明"做了什么 + 为什么"。
- 新增页面后**同步更新 `README.md` 的进度表**（用户把那张表当项目进度的事实来源）。

---

## 7. 已知的坑（踩过的，别再踩）

| 坑 | 说明 / 正确做法 |
| --- | --- |
| `Config.kt` 的 BASE_URL | 指向本机局域网 IP，属本地环境改动。**不要 `git add -A`**，只提交明确列出的文件 |
| Material3 1.1.x | 是 `rememberDismissState`/`SwipeToDismiss`，不是 1.2 的 `rememberSwipeToDismissState`；`Scaffold` 只有 `floatingActionButtonPosition = FabPosition.Center`，没有 `floatingActionButtonLocation` |
| `ModalBottomSheet` | 内部会吃掉底部 inset，`navigationBarsPadding()` 失效 → 自己补 `.padding(bottom = Dimens.middleGap * 2)` |
| 分页页码 | 用 `已加载条数 / pageSize + 1` 推算，不要 `pageNum + 1`（会重复拉同一页）；追加前按 id 去重 |
| 上拉加载 | 见 §6；另外 footer 文本用三态「加载中… / 上拉加载更多 / 已经到底了」 |
| `LaunchedEffect(key)` 自改 key | effect 内部把 key 置回 false 会重启并取消自己（后续代码不再执行）→ 用 `LaunchedEffect(稳定key){ flow.collect{...} }` |
| 跨页面通知刷新 | 用 `savedStateHandle` 打标记（写方 `getBackStackEntry(route).savedStateHandle[KEY]=true`，读方 collect 标记） |
| 新数据插入列表首位 | `LazyColumn` 按 key 锚定，新行会跑到视口上方 → 数据回来后再 `scrollToItem(0)` |
| Kotlin 字符串模板 + 中文 | `"$name功能开发中"` 会把中文吃进标识符 → 必须写 `"${name}功能开发中"` |
| 旋转动画暂停 | 用 `Animatable` + `LaunchedEffect(isPlaying)` 循环；不要 `rememberInfiniteTransition`（无法暂停、切换会跳回 0°） |
| 图片占位 | `MusicCover` 默认占位是蓝色唱片；歌手头像传 `fallbackRes = R.drawable.default_avater` |
| CJK 输入验证 | `adb shell input text` 打不了中文，测试数据用 ASCII |
| 点错坐标/没生效 | 先看 `uiautomator dump` 的 `package=` 是不是 `com.player.music`，再核对 tap 坐标来自**当前** dump |

---

## 8. 构建、运行、验证

```bash
# 构建（600s 内完成；不要用 | tail 过滤掉 e: 行）
./gradlew assembleDebug --console=plain > /tmp/build.log 2>&1; grep -E "^e: |FAILURE" /tmp/build.log

# 安装到模拟器（adb 不在 PATH）
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell am force-stop com.player.music
~/Library/Android/sdk/platform-tools/adb shell am start -n com.player.music/.MainActivity

# 界面断言用 uiautomator，别用截图猜坐标
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -o 'text="[^"]*"'
adb exec-out screencap -p > /tmp/shot.png

# 接口日志（tag：NetworkLog，URL 会重复打印两次，是同一请求）
adb logcat -s NetworkLog:* | grep -E "URL|Body"
```

仓库里没有单元测试/lint 任务，验证方式 = 编译通过 + 模拟器实际操作 + `uiautomator` 断言 + `logcat` 看接口。

---

## 9. 平行实现（移植页面时的参考）

| 客户端 | 路径 | 用途 |
| --- | --- | --- |
| Flutter | `~/Documents/code/flutter/flutter-music-app-ui/lib/pages/X.dart` | **视觉/布局**参考（注意有些页面是半成品：歌词页组件被注释、音乐圈没有悬浮加号、RecordMusicPage 没有入口） |
| uni-app Vue3 | `~/Documents/code/uniapp/uniapp-vite-vue3-ts-music-app-ui/src/{pages,components}` | **功能完整性**参考（Flutter 缺的功能通常这里实现了，如发布 FAB、选歌页、Markdown 渲染、`<music>` 歌单卡片） |
| 后端 | `~/Documents/code/python/fast-api-app-service`（`docs/api/*.md`） | 接口真实签名 / 字段，遇到"参数校验失败"先查这里 |
| 复用模板 | `~/Documents/code/android/andriod-jetpack-compose-chat-app` | 骨架/主题/DI 的来源项目 |

移植流程（推荐）：读 Dart → 与 Vue 对比补全 → 查后端接口真实参数 → 写 `ApiService` 方法 + Repository → ViewModel(`SavedStateHandle` 取参) → Compose 页面 → 注册路由 + 入口 → 编译 → 模拟器验证 → 补 README → 提交 gitee + github。

---

## 10. 已知缺口 / 未完成

- `NotFoundPage`：Flutter 有，Compose 用 NavHost 不需要，未实现。
- AI 聊天：未做 Markdown/HTML 富文本渲染（只做轻量符号清理），歌单卡片无收藏/点赞，文档上传未做。
- 收藏夹「重命名」：页面有编辑图标，但 Android 侧尚未封装 `updateFavoriteDirectory`，点击提示开发中。
- 后端数据问题：`getModelList` 返回的模型 `type = online`，后端 `_create_chat_model` 不识别，AI 对话会返回 `Error: 不支持的模型类型 online`（服务端问题，与客户端无关）。

---

## 11. 常见任务速查

| 任务 | 要动的文件 |
| --- | --- |
| 新增一个列表页 | `ApiService` 加接口 → `Repository` 加方法 → `viewmodel/XxxViewModel` → `ui/page/XxxPage`（用 `SongListCard`）→ `Screens` + `NavGraph` → 入口 → README |
| 给某页加"点歌播放" | 页面 `viewModel.play(index)`（内部 `player.playList(list, index)`）+ `navController.navigate(Screens.MusicPlayer.route)` |
| 改接口字段 | `model/*.kt`（`@SerializedName`）+ 用到的地方；字段位置以上面 §9 的后端 docs 为准 |
| 加一个可复用的 UI 块 | `ui/components/Xxx.kt`，参数传 `Modifier`，尺寸颜色用 `Dimens/Color` |
| 排查"页面没反应" | 先 `logcat -s NetworkLog:*` 看有没有请求；再看 `uiautomator dump` 节点是否存在（0×0 或缺失 = 布局问题） |

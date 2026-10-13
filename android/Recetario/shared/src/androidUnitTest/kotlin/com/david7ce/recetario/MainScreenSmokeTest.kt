package com.david7ce.recetario

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.david7ce.recetario.data.RecipeRepository
import com.david7ce.recetario.db.RecipeDatabase
import com.david7ce.recetario.platform.ImageCodec
import com.david7ce.recetario.platform.JvmZipCodec
import com.david7ce.recetario.platform.Services
import com.david7ce.recetario.platform.WebClient
import com.david7ce.recetario.ui.AppState
import com.david7ce.recetario.ui.PlatformUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private object NoOpImageCodec : ImageCodec {
    override fun normalize(bytes: ByteArray): ByteArray? = null
    override fun decode(bytes: ByteArray, maxSize: Int): ImageBitmap? = null
}

private object NoOpWebClient : WebClient {
    override suspend fun getText(url: String, maxBytes: Int): String = error("not used in this smoke test")
    override suspend fun getBytes(url: String, maxBytes: Int): ByteArray = error("not used in this smoke test")
}

private object NoOpPlatformUi : PlatformUi {
    override fun pickImage(onResult: (Result<ByteArray>?) -> Unit) = Unit
    override fun openFile(onResult: (Result<ByteArray>?) -> Unit) = Unit
    override fun saveFile(fileName: String, mimeType: String, bytes: ByteArray, onResult: (Boolean) -> Unit) = Unit
    override fun openUrl(url: String) = Unit
}

/** Catches the regressions that actually happen: a crash on launch, a broken top bar. */
@RunWith(RobolectricTestRunner::class)
class MainScreenSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun libraryScreenRendersItsTitle() {
        // The real Android driver (as production code uses), not the JVM JDBC one: DriverManager's
        // auto-registration doesn't cross Robolectric's sandboxed classloader.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val driver = AndroidSqliteDriver(RecipeDatabase.Schema, context, "smoke-test.db")
        val repo = RecipeRepository(RecipeDatabase(driver), MemoryImageStore(), { 1000L }, { "id" })
        val services = Services(
            images = MemoryImageStore(),
            codec = NoOpImageCodec,
            zip = JvmZipCodec(),
            web = NoOpWebClient,
            pdf = null,
            clock = { 1000L },
        )
        val state = AppState(services, repo, CoroutineScope(Dispatchers.Unconfined), systemLanguage = "en")

        compose.setContent { RecetarioApp(state, NoOpPlatformUi) }

        compose.onNodeWithText("Recetario").assertExists()
    }
}

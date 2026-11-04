package com.david7ce.calistenia.ui.figure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.david7ce.calistenia.resources.Res
import com.david7ce.calistenia.resources.jack_0
import com.david7ce.calistenia.resources.jack_1
import com.david7ce.calistenia.resources.jack_2
import com.david7ce.calistenia.resources.jack_3
import com.david7ce.calistenia.resources.jack_4
import com.david7ce.calistenia.resources.jack_5
import com.david7ce.calistenia.resources.jack_6
import com.david7ce.calistenia.resources.jack_7
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A movement rendered offline, played back as a flipbook.
 *
 * The drawn [Figure] stays the fallback and the default: it works for every
 * family, costs nothing, and follows the theme. A flipbook exists for the
 * exercises where a rendered body reads better than a stick figure, and it is
 * deliberately a *separate* type rather than a branch inside [Figure] — the
 * frames come from charstudio (see `refs/jack-prototype` in the AgentHub workspace) and
 * are replaced wholesale when the art improves.
 *
 * Frames must be the same size; they are scaled to fit, not cropped.
 */
data class Flipbook(val frames: List<DrawableResource>, val periodMs: Int)

/** Jumping jacks: eight frames, the one movement this rig renders correctly. */
val JumpingJack = Flipbook(
    frames = listOf(
        Res.drawable.jack_0,
        Res.drawable.jack_1,
        Res.drawable.jack_2,
        Res.drawable.jack_3,
        Res.drawable.jack_4,
        Res.drawable.jack_5,
        Res.drawable.jack_6,
        Res.drawable.jack_7,
    ),
    periodMs = 1400,
)

/** The flipbook for a family, or null when the drawn figure is the right answer. */
fun flipbookFor(parentId: String): Flipbook? = when (parentId) {
    "builtin:jumping-jack" -> JumpingJack
    else -> null
}

@Composable
fun FlipbookFigure(
    flipbook: Flipbook,
    playing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (flipbook.frames.isEmpty()) return
    var index by remember(flipbook) { mutableIntStateOf(0) }

    LaunchedEffect(flipbook, playing) {
        if (!playing || flipbook.frames.size < 2) return@LaunchedEffect
        while (true) {
            delay((flipbook.periodMs / flipbook.frames.size).toLong())
            index = (index + 1) % flipbook.frames.size
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(flipbook.frames[index]),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

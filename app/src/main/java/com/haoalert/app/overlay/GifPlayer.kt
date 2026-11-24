package com.haoalert.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** View do GIF + função para liberar a reprodução e a memória quando a sobreposição sair da tela. */
class GifHandle(val view: View, private val onRelease: () -> Unit) {
    fun release() = onRelease()
}

/**
 * Reproduz o arquivo ORIGINAL `assets/tung-tung-sahur.gif`, sem converter para imagem estática.
 * - Android 9+ : ImageDecoder + AnimatedImageDrawable (decodifica quadro a quadro, usa a GPU/RenderThread).
 * - Android 8.x: android.graphics.Movie.
 * Só existe uma instância por vez (há uma única sobreposição), e ela é liberada ao fechar.
 */
object GifPlayer {
    const val ASSET_NAME = "tung-tung-sahur.gif"

    /** Retorna null se o arquivo não estiver no app ou não puder ser decodificado. */
    fun create(context: Context): GifHandle? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) createAnimatedDrawable(context)
        else createMovie(context)
    } catch (_: Throwable) {
        null
    }

    private fun createAnimatedDrawable(context: Context): GifHandle {
        val source = ImageDecoder.createSource(context.assets, ASSET_NAME)
        val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
            // Limita o tamanho decodificado (nunca amplia): menos memória por quadro, proporção preservada.
            val longest = max(info.size.width, info.size.height).toFloat()
            val scale = min(1f, MAX_DECODE_SIDE / longest)
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER // sem distorcer
            setImageDrawable(drawable)
        }
        val animated = drawable as? AnimatedImageDrawable
        animated?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        animated?.start()
        return GifHandle(imageView) {
            animated?.stop()
            imageView.setImageDrawable(null)
        }
    }

    @Suppress("DEPRECATION")
    private fun createMovie(context: Context): GifHandle {
        val movie = context.assets.open(ASSET_NAME).use { Movie.decodeStream(it) }
            ?: throw IllegalStateException("GIF inválido")
        val view = MovieView(context, movie)
        return GifHandle(view) { view.stopPlayback() }
    }

    private const val MAX_DECODE_SIDE = 720f
}

/** Reprodução de GIF para Android 8.x. Para de redesenhar assim que sai da tela. */
@Suppress("DEPRECATION")
private class MovieView(context: Context, private val movie: Movie) : View(context) {
    private var startTime = 0L
    private var running = true

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Movie exige renderização por software
    }

    override fun onDraw(canvas: Canvas) {
        if (!running || movie.width() == 0 || movie.height() == 0) return
        val duration = movie.duration().takeIf { it > 0 } ?: 1000
        if (startTime == 0L) startTime = SystemClock.uptimeMillis()
        movie.setTime(((SystemClock.uptimeMillis() - startTime) % duration).toInt())

        val scale = min(width / movie.width().toFloat(), height / movie.height().toFloat())
        val dx = (width - movie.width() * scale) / 2f
        val dy = (height - movie.height() * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        movie.draw(canvas, 0f, 0f)
        canvas.restore()
        postInvalidateOnAnimation()
    }

    fun stopPlayback() {
        running = false
    }
}

package com.aurora.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap
) {
    val key: String get() = "$packageName/$activityName"
}

object AppRepo {

    suspend fun load(context: Context): List<AppEntry> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val size = (64 * context.resources.displayMetrics.density).toInt().coerceAtLeast(128)

        pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    icon = renderIcon(ri.loadIcon(pm), size).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Packages of the default phone, messages, browser and camera apps (used for the dock). */
    fun dockPackages(context: Context): List<String> {
        val pm = context.packageManager
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )
        return probes
            .mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .filter { it != "android" && it != context.packageName }
            .distinct()
    }

    /**
     * Draws the icon's own layers full-bleed so every app ends up with the same
     * rounded-square shape (applied later in Compose), instead of the phone's circle/teardrop mask.
     */
    private fun renderIcon(drawable: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (drawable is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp, of which the central 72dp is the visible area.
            val inset = (size * 0.25f).toInt()
            drawable.background?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(canvas)
            }
            drawable.foreground?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(canvas)
            }
        } else {
            canvas.drawColor(0xFFF2F2F7.toInt())
            val pad = (size * 0.14f).toInt()
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.draw(canvas)
        }
        return bmp
    }
}

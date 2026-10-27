package ir.rosheno.launcher.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri

/** Bound memory usage while preserving aspect ratio and avoiding image upscaling. */
fun decodeBoundedBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap {
    require(maxSide > 0)
    return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val scale = minOf(1f, maxSide.toFloat() / maxOf(info.size.width, info.size.height))
        decoder.setTargetSize(
            maxOf(1, (info.size.width * scale).toInt()),
            maxOf(1, (info.size.height * scale).toInt()),
        )
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
}

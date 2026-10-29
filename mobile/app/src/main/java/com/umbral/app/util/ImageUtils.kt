package com.umbral.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun comprimirImagenUri(context: Context, imageUri: Uri, calidad: Int = 70): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val outputFile = File(context.cacheDir, "temp_compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, byteArrayOutputStream)
            outputStream.write(byteArrayOutputStream.toByteArray())
            outputStream.flush()
            outputStream.close()

            outputFile
        } catch (e: Exception) {
            null
        }
    }
}

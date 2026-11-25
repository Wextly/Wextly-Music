package com.wextly.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Хранит библиотеку в файле library.json и читает данные о треках (название, автор, обложка). */
class LibraryStore(private val context: Context) {

    private val libraryFile: File
        get() = File(context.filesDir, "library.json")

    private val artDir: File
        get() = File(context.filesDir, "art").apply { mkdirs() }

    fun load(): List<Track> {
        val file = libraryFile
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Track(
                    id = o.getString("id"),
                    uri = o.getString("uri"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    durationMs = o.optLong("duration"),
                    artPath = o.optString("art").ifEmpty { null },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(tracks: List<Track>) {
        val arr = JSONArray()
        tracks.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("uri", t.uri)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("duration", t.durationMs)
                    .put("art", t.artPath ?: "")
            )
        }
        libraryFile.writeText(arr.toString())
    }

    /** Читает теги файла. Возвращает null, если файл не удалось открыть. */
    fun readTrack(uri: Uri): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val id = UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: displayName(uri)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val art = retriever.embeddedPicture?.let { saveArt(id, it) }
            Track(id, uri.toString(), title, artist, duration, art)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ничего: файл уже закрыт
            }
        }
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name.substringBeforeLast('.')
                }
            }
        return "Трек"
    }

    /** Сохраняет обложку из тегов в уменьшенном виде и возвращает путь к файлу. */
    private fun saveArt(id: String, bytes: ByteArray): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1024) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap == null) {
                null
            } else {
                val out = File(artDir, "$id.jpg")
                out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                out.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }
}

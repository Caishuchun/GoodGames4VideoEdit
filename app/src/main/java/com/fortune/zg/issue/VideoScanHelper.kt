package com.fortune.zg.issue

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import com.alibaba.sdk.android.tbrest.utils.MD5Utils
import java.io.File
import java.io.FileOutputStream

object VideoScanHelper {
    private val MEDIA_PICK_TEMP_FOLDER_PATH =
        Environment.getExternalStorageDirectory().absolutePath + "/mediaPicker/temp/";

    fun start(context: Context, handler: Handler) {
        Thread {
            doScan(context, handler)
        }.start()
    }

    private fun doScan(context: Context, handler: Handler) {
        val mediaColumns: Array<String> = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.ALBUM
        )

        val sortOrder: String = MediaStore.Video.Media.DATE_ADDED + " desc"
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaColumns,
                null,
                null,
                sortOrder
            )
            cursor?.moveToFirst().apply {
                when (this) {
                    true -> parseData(context, cursor!!, handler)
                }
            }
        } finally {
            cursor?.close()
        }
    }

    private fun parseData(context: Context, cursor: Cursor, handler: Handler) {
        val thumbColumns: Array<String> = arrayOf(
            MediaStore.Video.Thumbnails.DATA,
            MediaStore.Video.Thumbnails.VIDEO_ID
        )
        do {
            val id = ScanResultUtil.getLongResultByKey(cursor, MediaStore.Video.Media._ID)
            val filePath = ScanResultUtil.getStringResultByKey(cursor, MediaStore.Video.Media.DATA)
            val mimeType =
                ScanResultUtil.getStringResultByKey(cursor, MediaStore.Video.Media.MIME_TYPE)
            val duration =
                ScanResultUtil.getLongResultByKey(cursor, MediaStore.Video.Media.DURATION)
            val createTime =
                ScanResultUtil.getLongResultByKey(cursor, MediaStore.Video.Media.DATE_ADDED)
            val albumName = ScanResultUtil.getAlbumNameFromPath(filePath)

            val thumbSelection = MediaStore.Video.Thumbnails.VIDEO_ID + "=?"
            val thumbSelectionArgs = arrayOf(id.toString())

            var thumbCursor: Cursor? = null
            var thumbNailPath: String? = null

            try {
                thumbCursor = context.contentResolver.query(
                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    thumbColumns, thumbSelection, thumbSelectionArgs, null
                )

                thumbCursor?.moveToFirst().let {
                    when (it) {
                        true -> thumbNailPath = ScanResultUtil.getStringResultByKey(
                            thumbCursor!!,
                            MediaStore.Video.Thumbnails.DATA
                        )
                    }
                }

                if (thumbNailPath.isNullOrEmpty()) {
                    thumbNailPath = generateThumbNail(filePath)
                }
                val mediaData = MediaData(
                    id,
                    createTime,
                    duration,
                    albumName,
                    filePath,
                    thumbNailPath,
                    mimeType,
                    null,
                    null
                )
                val msg: Message = Message.obtain()
                msg.obj = mediaData
                msg.what = MediaType.MEDIA_TYPE_VIDEO
                handler.sendMessage(msg)
            } finally {
                thumbCursor?.close()
            }

        } while (cursor.moveToNext())

        val msg: Message = Message.obtain()
        msg.what = -1
        handler.sendMessage(msg)
    }

    private fun generateThumbNail(filePath: String?): String? {
        filePath?.let {
            var out: FileOutputStream? = null
            try {
                val fileName = "${MD5Utils.getMd5Hex(filePath.toByteArray())}.jpg"
                val folder = File(MEDIA_PICK_TEMP_FOLDER_PATH)
                if (!folder.exists()) {
                    folder.mkdirs()
                }

                val file = File(MEDIA_PICK_TEMP_FOLDER_PATH.plus(fileName))
                if (!file.exists()) {
                    val bitmap = createVideoThumbnail(filePath)
                    bitmap?.let {
                        out = FileOutputStream(file)
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            50,
                            out
                        )//??????????????????????????????????????????????????????100??????png???????????????
                        out?.flush()
                        bitmap.recycle()
                    }
                }
                return file.absolutePath
            } catch (e: Exception) {
            } finally {
                out?.close()
            }
        }
        return null
    }

    private fun createVideoThumbnail(filePath: String): Bitmap? {
        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            bitmap = retriever.getFrameAtTime(0)
        } catch (e: Exception) {

        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {

            }
        }

        bitmap?.let {
            val width: Int = it.width
            val height: Int = it.height
            val max: Int = Math.max(width, height)
            if (max > 512) {
                val scale: Float = 512f / max
                val w = Math.round(scale * width)
                val h = Math.round(scale * height)
                bitmap = Bitmap.createScaledBitmap(it, w, h, true)
            }
        }
        return bitmap
    }
}
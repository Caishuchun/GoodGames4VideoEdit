package com.fortune.zg.issue

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.fortune.zg.R
import com.fortune.zg.utils.LogUtils
import com.google.gson.Gson


object LocalMusicUtil {
    private var localMusicList = mutableSetOf<MusicListBean.Data.MusicList>()

    /**
     * 获取本地音乐库
     */
    fun getLocalMust(context: AppCompatActivity): MutableSet<MusicListBean.Data.MusicList> {
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                var name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                if (name.contains("-")) {
                    val split = name.split("-")
                    name = split[split.size - 1]
                    name = name.substring(0, name.lastIndexOf(".")).trim()
                }
                val id =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val author =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val duration =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val cover =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                if (duration != null) {
                    val song = MusicListBean.Data.MusicList(
                        author,
                        cover,
                        duration.toInt(),
                        0,
                        -2,
                        name,
                        path
                    )
                    LogUtils.d("Song=>${Gson().toJson(song)}")
                    localMusicList.add(song)
                }
            }
        }
        cursor?.close()
        return localMusicList
    }

    /**
     * 获取封面
     */
    fun getCover(context: AppCompatActivity, path: String): Bitmap? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(path)
        val embeddedPicture = mediaMetadataRetriever.embeddedPicture
        var bitmap: Bitmap? = null
        if (embeddedPicture != null) {
            bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            val width = bitmap.width
            val height = bitmap.height
//            LogUtils.d("getCover=>width:$width,height:$height")
            val matrix = Matrix()
            //计算缩放比例
            val sx = 120.0f / width
            val sy = 120.0f / height
            matrix.postScale(sx, sy)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
            return bitmap
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.icon)
            val width = bitmap.width
            val height = bitmap.height
            LogUtils.d("getCover=>width:$width,height:$height")
            val matrix = Matrix()
            //计算缩放比例
            val sx = 120.0f / width
            val sy = 120.0f / height
            matrix.postScale(sx, sy)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
            return bitmap
        }
    }
}
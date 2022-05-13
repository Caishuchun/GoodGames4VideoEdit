package com.fortune.zg.issue

import android.media.MediaMetadataRetriever
import com.google.gson.Gson

object MediaInfoUtil {

    /**
     * 获取视频宽度
     */
    fun getMediaWidth4Video(mediaInfo: String): Int {
        val format = format4Video(mediaInfo)
        val rotation = getRotation(format.url)
        return if (rotation == 0) {
            format.videostream_codecpar_width.toInt()
        } else {
            format.videostream_codecpar_height.toInt()
        }
    }

    /**
     * 获取视频宽度
     */
    fun getMediaHeight4Video(mediaInfo: String): Int {
        val format = format4Video(mediaInfo)
        val rotation = getRotation(format.url)
        return if (rotation == 0) {
            format.videostream_codecpar_height.toInt()
        } else {
            format.videostream_codecpar_width.toInt()
        }
    }

    /**
     * 0竖屏,其他横屏
     */
    private fun getRotation(path: String): Int {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(path)
        val rotation =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        return rotation?.toInt() ?: 0
    }

    /**
     * 获取视频时长
     */
    fun getMediaDuration4Video(mediaInfo: String): Long {
        val format = format4Video(mediaInfo)
        return format.duration.split(" ")[0].split(".")[0].toLong()
    }

    /**
     * 格式视频jsonBean
     */
    fun format4Video(mediaInfo: String): MediaInfoBean {
        var replace = mediaInfo.replace("\\u003d", "=").replace("\n", "")
        replace = toDeleteRubbish(replace)
        val split = replace.split(";")
        var result = "{"
        for (index in split.indices) {
            val split1 = split[index].split("=")
            result += if (split1.size == 1) {
                "\"${split1[0]}\":\"null\""
            } else {
                "\"${split1[0]}\":\"${split1[1]}\""
            }
            result += if (index == split.size - 1) {
                ""
            } else {
                ","
            }
        }
        result += "}"
        return Gson().fromJson(result, MediaInfoBean::class.java)
    }

    /**
     * 删除垃圾数据
     * 比如说是一些APP自己添加的数据,常见如抖音快手视频会有类似于 "com.kuaishou":"{"a":"b","c":"d","e":"{"f":"g"}"}"
     */
    private fun toDeleteRubbish(replace: String): String {
        var start = 0
        var end = 0
        var isStart = false
        var temp = 0
        for (index in replace.indices) {
            if (replace[index].toString() == "{") {
                if (isStart) {
                    temp++
                } else {
                    start = index
                    isStart = true
                }
            } else if (replace[index].toString() == "}") {
                if (isStart) {
                    if (temp == 0) {
                        end = index
                    } else {
                        temp--
                    }
                }
            }
        }
        return if (end == 0) {
            replace
        } else {
            toDeleteRubbish(replace.replace(replace.substring(start, end + 1), ""))
        }
    }
}
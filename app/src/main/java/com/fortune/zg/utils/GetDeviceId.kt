package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.telephony.TelephonyManager
import com.fortune.zg.constants.SPArgument
import java.io.*
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.*


object GetDeviceId {
    //保存文件的路径
    private const val CACHE_IMAGE_DIR = "hfdd/cache/devices"

    //保存的文件 采用隐藏文件的形式进行保存
    private const val DEVICES_FILE_NAME = ".DEVICES"

    /**
     * 获取设备唯一标识符
     * @param context
     * @return
     */
    fun getDeviceId(context: Context): String {
        //先读Sp
        var deviceId = SPUtils.getString(SPArgument.ONLY_DEVICE_ID, null)
        if (deviceId != null) {
            return deviceId
        }
        //读取保存的在sd卡中的唯一标识符
        deviceId = readDeviceID(context)
        //用于生成最终的唯一标识符
        val s = StringBuffer()
        //判断是否已经生成过,
        if (deviceId != null && "" != deviceId) {
            return deviceId
        }
        try {
            //获取IMES(也就是常说的DeviceId)
            deviceId = getIMIEStatus(context)
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            //获取设备的MACAddress地址 去掉中间相隔的冒号
            deviceId = getLocalMac(context).replace(":", "")
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //如果以上搜没有获取相应的则自己生成相应的UUID作为相应设备唯一标识符
        if (s.isEmpty()) {
            val uuid = UUID.randomUUID()
            deviceId = uuid.toString().replace("-", "")
            s.append(deviceId)
        }

        //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        val md5 = getMD5(s.toString(), false)
        val onlyDeviceId = getShortUrl(md5)
        if (s.isNotEmpty()) {
            //持久化操作, 进行保存到SD卡中
            saveDeviceID(onlyDeviceId, context)
            SPUtils.putValue(SPArgument.ONLY_DEVICE_ID, onlyDeviceId)
        }
        return onlyDeviceId
    }

    /**
     * 读取固定的文件中的内容,这里就是读取sd卡中保存的设备唯一标识符
     * @param context
     * @return
     */
    private fun readDeviceID(context: Context): String? {
        val file = getDevicesDir(context)
        val buffer = StringBuffer()
        return try {
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis, "UTF-8")
            val `in`: Reader = BufferedReader(isr)
            var i: Int
            while (`in`.read().also { i = it } > -1) {
                buffer.append(i.toChar())
            }
            `in`.close()
            buffer.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取设备的DeviceId(IMES) 这里需要相应的权限
     * 需要 READ_PHONE_STATE 权限
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    private fun getIMIEStatus(context: Context): String {
        val tm = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.deviceId
    }

    /**
     * 获取设备MAC 地址 由于 6.0 以后 WifiManager 得到的 MacAddress得到都是 相同的没有意义的内容
     * 所以采用以下方法获取Mac地址
     * @param context
     * @return
     */
    private fun getLocalMac(context: Context): String {
        var macAddress: String? = null
        val buf = StringBuffer()
        var networkInterface: NetworkInterface? = null
        try {
            networkInterface = NetworkInterface.getByName("eth1")
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0")
            }
            if (networkInterface == null) {
                return ""
            }
            val addr = networkInterface.hardwareAddress
            for (b in addr) {
                buf.append(String.format("%02X:", b))
            }
            if (buf.isNotEmpty()) {
                buf.deleteCharAt(buf.length - 1)
            }
            macAddress = buf.toString()
        } catch (e: SocketException) {
            e.printStackTrace()
            return ""
        }
        return macAddress
    }

    /**
     * 保存 内容到 SD卡中, 这里保存的就是 设备唯一标识符
     * @param str
     * @param context
     */
    private fun saveDeviceID(str: String?, context: Context) {
        val file = getDevicesDir(context)
        try {
            val fos = FileOutputStream(file)
            val out: Writer = OutputStreamWriter(fos, "UTF-8")
            out.write(str)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 对挺特定的 内容进行 md5 加密
     * @param message 加密明文
     * @param upperCase 加密以后的字符串是是大写还是小写 true 大写 false 小写
     * @return
     */
    private fun getMD5(message: String, upperCase: Boolean): String {
        var md5str = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val input = message.toByteArray()
            val buff = md.digest(input)
            md5str = bytesToHex(buff, upperCase)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return md5str
    }

    /**
     * byte转16进制
     * @param bytes     byte数组
     * @param upperCase 大小写
     */
    private fun bytesToHex(bytes: ByteArray, upperCase: Boolean): String {
        val md5str = StringBuffer()
        var digital: Int
        for (i in bytes.indices) {
            digital = bytes[i].toInt()
            if (digital < 0) {
                digital += 256
            }
            if (digital < 16) {
                md5str.append("0")
            }
            md5str.append(Integer.toHexString(digital))
        }
        return if (upperCase) {
            md5str.toString().toUpperCase(Locale.ROOT)
        } else md5str.toString().toLowerCase(Locale.ROOT)
    }

    /**
     * 统一处理设备唯一标识 保存的文件的地址
     * @param context
     * @return
     */
    private fun getDevicesDir(context: Context): File {
        var mCropFile: File? = null
        mCropFile = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val cropdir = File(Environment.getExternalStorageDirectory(), CACHE_IMAGE_DIR)
            if (!cropdir.exists()) {
                cropdir.mkdirs()
            }
            File(cropdir, DEVICES_FILE_NAME) // 用当前时间给取得的图片命名
        } else {
            val cropdir = File(context.filesDir, CACHE_IMAGE_DIR)
            if (!cropdir.exists()) {
                cropdir.mkdirs()
            }
            File(cropdir, DEVICES_FILE_NAME)
        }
        return mCropFile
    }

    /**
     * 短网址生成方法
     * 这个方法会,生成四个短字符串,每一个字符串的长度为6
     * 这个方法是从网上搜索的一个方法,但不知道出自何处了,稍微将key换了一下
     * @param url
     * @return
     */
    private fun shortUrl(url: String): Array<String?> {
        // 可以自定义生成 MD5 加密字符传前的混合 KEY
        var key = UUID.randomUUID().toString()

        //混淆key,加上当前时间,并且取一个随机字符串
        key = System.currentTimeMillis().toString() + key

        // 要使用生成 URL 的字符
        val chars = arrayOf(
            "a", "b", "c", "d", "e", "f", "g", "h",
            "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
            "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
        )

        val resUrl = arrayOfNulls<String>(4)
        for (i in 0..3) {
            // 把加密字符按照 8 位一组 16 进制与 0x3FFFFFFF 进行位与运算
            val sTempSubString = url.substring(i * 8, i * 8 + 8)

            // 这里需要使用 long 型来转换，因为 Inteper .parseInt() 只能处理 31 位 , 首位为符号位 , 如果不用long ，则会越界
            var lHexLong = (0x3FFFFFFF and sTempSubString.toLong(16).toInt()).toLong()
            var outChars = ""
            for (j in 0..5) {
                // 把得到的值与 0x0000003D 进行位与运算，取得字符数组 chars 索引
                val index = (0x0000003D and lHexLong.toInt()).toLong()

                // 把取得的字符相加
                outChars += chars[index.toInt()]

                // 每次循环按位右移 5 位
                lHexLong = lHexLong shr 5
            }

            // 把字符串存入对应索引的输出数组
            resUrl[i] = outChars
        }
        return resUrl
    }

    /**
     * 获取我想要的字符串,将生成的两个相加,得到我想要的12位字符
     * @param url
     * @return
     */
    private fun getShortUrl(url: String): String {
        val aResult = shortUrl(url)
        return aResult[0] + aResult[1]
    }
}
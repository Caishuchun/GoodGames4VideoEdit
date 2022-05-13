package com.fortune.zg.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.fortune.zg.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern


/**
 * Author: 蔡小树
 * Time: 2019/12/19 11:26
 * Description:
 */

object OtherUtils {
    /**
     * 检查是是不是正确的手机号
     * @param num 手机号
     */
    fun isPhone(num: String): Boolean {
        val regExp = "^((13[0-9])|(15[^4])|(18[0-9])|(17[0-8])|(14[5-9])|(166)|(19[8,9])|)\\d{8}$"
        val p = Pattern.compile(regExp)
        val m = p.matcher(num)
        return m.matches()
    }

    /**
     * 是否是合格的密码
     */
    fun isPassword(password: String): Boolean {
        val regExp = "(.*[a-zA-Z].*[0-9]|.*[0-9].*[a-zA-Z])"
        val p = Pattern.compile(regExp)
        val m = p.matcher(password)
        return m.matches()
    }

    /**
     * 是否是合格的身份证
     */
    fun isIdCardNum(idCard: String): Boolean {
        val regExp = "^\\d{15}$|^\\d{17}[0-9Xx]$"
        val p = Pattern.compile(regExp)
        val m = p.matcher(idCard)
        return m.matches()
    }

    /**
     * 是否是合格的日期
     */
    fun isDate(date: String): Boolean {
        val regExp =
            "^((([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29))\$"
        val p = Pattern.compile(regExp)
        val m = p.matcher(date)
        return m.matches()
    }

    /**
     * 是否是合格的邮件
     */
    fun isEmail(email: String): Boolean {
        val regExp = "\\w[-\\w.+]*@([A-Za-z0-9][-A-Za-z0-9]+\\.)+[A-Za-z]{2,14}"
        val p = Pattern.compile(regExp)
        val m = p.matcher(email)
        return m.matches()
    }

    /**
     * 是否是个合规的网址
     */
    fun isUrl(url: String): Boolean {
        val regexStr: StringBuilder = StringBuilder(
            "^((https|http|ftp|rtsp|mms)?://)"  //https、http、ftp、rtsp、mms
                    + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
                    + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184
                    + "|" // 允许IP和DOMAIN（域名）
                    + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.
                    + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
                    + "[a-z]{2,6})" // first level domain- .com or .museum
                    + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
                    + "((/?)|" // a slash isn't required if there is no file name
                    + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$"
        )
        val regex = Regex(regexStr.toString())
        return url.lowercase(Locale.getDefault()).matches(regex)
    }

    /**
     * 判断文字是否违规
     * @return 1包含网址,2字符违规,0正常
     */
    fun isGotOutOfLine(context: Context, str: String): Int {
        val urlRegex: StringBuilder = StringBuilder(
            "^((https|http|ftp|rtsp|mms)?://)"  //https、http、ftp、rtsp、mms
                    + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
                    + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184
                    + "|" // 允许IP和DOMAIN（域名）
                    + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.
                    + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
                    + "[a-z]{2,6})" // first level domain- .com or .museum
                    + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
                    + "((/?)|" // a slash isn't required if there is no file name
                    + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$"
        )
        val p = Pattern.compile(urlRegex.toString())
        val matcher = p.matcher(str)
        if (matcher.find()) {
            return 1
        }
        val userNameRules = Regex("^((?!(微信|wx|WX|\\+V|\\+v|\\+微|QQ|qq|[0-9]{6,})).)*$")
        if (!str.matches(userNameRules)) {
            return 2
        }
        val inputStream = context.assets.open("get_out_of_line.txt")
        val br = BufferedReader(InputStreamReader(inputStream))
        while (true) {
            val line = br.readLine()
            if (line != null && line != "") {
                if (str.contains(line)) {
                    return 2
                }
            } else {
                break
            }
        }
        return 0
    }

    /**
     * 显示软键盘
     * @param context
     * @param view 承接的View
     */
    fun showSoftKeyboard(context: Context, view: EditText) {
        LogUtils.d("++++++++++++++++showSoftKeyboard")
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.postDelayed({
            view.requestFocus()
            val inputMethodManager =
                view.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//            inputMethodManager.showSoftInput(view, 0)
            inputMethodManager.toggleSoftInput(0,0)
        }, 500)
    }

    /**
     * 强制隐藏软键盘
     * @param context
     * @param view 承接的View
     */
    fun hindKeyboard(context: Context, view: View) {
        val inputMethodManager =
            context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val active = inputMethodManager.isActive
        if (active) {
            Handler().postDelayed({
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }, 100)
        }
    }

    /**
     * 复制到粘贴板
     */
    fun copy(context: Context, msg: String) {
        val clipData = ClipData.newPlainText("", msg)
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clipData)
        ToastUtils.show(context.getString(R.string.copy_success))
    }

    /**
     * 分享 text
     * @param msg 要分享的文本数据
     */
    fun share(context: Context, msg: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, msg)
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share)
            )
        )
    }

    /**
     * 发送短信
     * @param phones 手机号列表
     * @param msg 要发送的信息
     */
    fun sendSMS(context: Context, phones: List<String>, msg: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SENDTO
        val phoneStr = StringBuffer()
        for (phone in phones) {
            phoneStr.append(";$phone")
        }
        intent.data = Uri.parse("smsto:${phoneStr.substring(1, phoneStr.length)}")
        //设置短信的默认发送内容
        intent.putExtra("sms_body", msg)
        context.startActivity(intent)
    }

    /**
     * px转dp
     */
    fun px2dp(context: Context, pxValue: Float): Int {
        val density = context.resources.displayMetrics.density
        return (pxValue / density + 0.5f).toInt()
    }
}
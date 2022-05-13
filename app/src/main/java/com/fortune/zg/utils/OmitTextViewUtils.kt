package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.widget.TextView

/**
 * 多余字省略工具类
 */
object OmitTextViewUtils {

    @SuppressLint("SetTextI18n")
    fun omitTextView(textView: TextView, str: String, maxLength: Int) {
        if(str.length>maxLength){
            textView.text = "${str.substring(0,maxLength)}..."
        }else{
            textView.text = str
        }
    }
}
package com.fortune.proxylib

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent

interface ProxyInterface {
    fun onCreate(savedInstanceState: Bundle?)
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroy()
    fun onSaveInstanceState(outState: Bundle?)
    fun onTouchEvent(event: MotionEvent?): Boolean
    fun onAttach(context: Activity?)
}
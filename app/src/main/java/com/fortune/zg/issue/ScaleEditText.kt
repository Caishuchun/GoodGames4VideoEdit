package com.fortune.zg.issue

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils

class ScaleEditText : androidx.appcompat.widget.AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    )

    init {
        gravity = Gravity.CENTER
        background = null
        isCursorVisible = false
        setLineSpacing(1f, 0.8f)

        hint = "可双击编辑"
        setHintTextColor(Color.WHITE)
        setTextColor(Color.WHITE)

        this.isLongClickable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            toChangeTextSize()
        }
    }

    private var isTextSizeChange = true

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (isTextSizeChange) {
            isTextSizeChange = false
            toChangeTextSize()
        }
    }

    /**
     * 修改文字大小
     */
    private fun toChangeTextSize() {
        val length = if (text.isNullOrEmpty()) {
            5
        } else {
            text.toString().trim().length
        }
        val canUseWidth = width - ScreenUtils.getScreenWidth(context).toFloat() / 360 * 16 * 0.85f
        val canUseHeight =
            (height - ScreenUtils.getScreenWidth(context).toFloat() / 360 * 16) * 0.85f
        val allArea = canUseHeight * canUseWidth
        val textArea = allArea / (if (length % 2 == 0) length else length + 1)
        var singeLineTextSize = (Math.sqrt(textArea.toDouble())).toFloat()

        while (true) {
            val oneLineTextNum = (canUseWidth / singeLineTextSize).toInt()
            var textLines = length / oneLineTextNum
            if (length % oneLineTextNum != 0) {
                textLines += 1
            }
            if (singeLineTextSize * textLines > canUseHeight) {
                singeLineTextSize *= 0.85f
            } else {
                isTextSizeChange = true
                break
            }
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, singeLineTextSize)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            isFocusable = true
            super.onTouchEvent(event)
        }
        return false
    }

    fun showOrHide(isShow: Boolean) {
        visibility = if (isShow) View.VISIBLE else View.GONE
        mMoveLayout?.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    private var mMoveLayout: MoveLayout? = null
    fun setSelfMoveLayout(moveLayout: MoveLayout) {
        mMoveLayout = moveLayout
    }

    fun getSelfMoveLayout() = mMoveLayout

    private var pages = mutableListOf<Int>()
    fun setPage(page: Int) {
        if (pages.contains(page)) {
            pages.remove(page)
        } else {
            pages.add(page)
        }
        pages.sort()
    }

    fun getPages() = pages

}
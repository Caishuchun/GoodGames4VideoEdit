package com.fortune.zg.bean

class AllCodeRecordsBean {
    private var code: Int? = null
    private var msg: String? = null
    private var data: List<DataBean?>? = null

    companion object {
        private var mDate: MutableList<DataBean?>? = null

        fun getData() = mDate
        fun setData(data: MutableList<DataBean?>?) {
            this.mDate = data
        }

        fun removeData() {
            if (this.mDate != null && this.mDate?.isNotEmpty() == true) {
                this.mDate?.removeAt(0)
            }
        }

        fun clear() {
            this.mDate = null
        }
    }


    fun getCode(): Int? {
        return code
    }

    fun setCode(code: Int?) {
        this.code = code
    }

    fun getMsg(): String? {
        return msg
    }

    fun setMsg(msg: String?) {
        this.msg = msg
    }

    fun getData(): List<DataBean?>? {
        return data
    }

    fun setData(data: List<DataBean?>?) {
        this.data = data
    }

    class DataBean {
        var user_phone: String? = null
        var game_name: String? = null
        var game_id: Int? = null
        var game_cover: String? = null
        var game_badge: String? = null
        var video_id: Int? = null
        var video_pos: Int? = null
        var video_name: String? = null
    }
}
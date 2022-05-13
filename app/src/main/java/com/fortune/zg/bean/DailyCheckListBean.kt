package com.fortune.zg.bean

class DailyCheckListBean {

    private var code: Int? = null
    private var msg: String? = null
    private var data: DataBean? = null

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

    fun getData(): DataBean? {
        return data
    }

    fun setData(data: DataBean?) {
        this.data = data
    }

    class DataBean {
        var list: List<ListBean>? = null
        var is_clock_in: Int? = null

        class ListBean {
            var type: Int? = null
            var num: Int? = null
            var status: Int? = null
        }
    }
}
package com.fortune.zg.bean

class ReplayCommonListBean {
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
        var paging: PagingBean? = null

        class PagingBean {
            var page: Int? = null
            var limit: Int? = null
            var count: Int? = null
        }

        class ListBean {
            var user_name: String? = null
            var user_avatar: String? = null
            var reply_content: String? = null
            var reply_time: Int? = null
        }
    }
}
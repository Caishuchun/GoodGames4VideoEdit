package com.fortune.zg.bean

class GetShareListBean {
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
        var share_reward: Int? = null
        var invite_reward: Int? = null
        var max_level: Int? = null
        var list: List<ListBean>? = null

        class ListBean {
            var id: Int? = null
            var receive: Int? = null
            var channel: String? = null
            var user: UserBean? = null

            class UserBean {
                var user_name: String? = null
                var user_avatar: String? = null
                var user_level: Int? = null
            }
        }
    }
}
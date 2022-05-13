package com.fortune.zg.bean

class UserInfoBean {

    companion object {
        private var mDate: Data? = null

        fun getData() = mDate
        fun setData(data: Data) {
            this.mDate = data
        }

        fun clear() {
            this.mDate = null
        }
    }

    var code = 0
    var msg: String? = null
    var data: Data? = null

    inner class Data {
        var user_id: String? = null
        var user_name: String? = null
        var user_desc: String? = null
        var user_sex = 0
        var user_birthday: String? = null
        var user_avatar: String? = null
        var avatar_path: String? = null
        var user_phone: String? = null
        var user_integral: Int? = null
        var user_level: Int? = null
        var user_now_exp: Int? = null
        var user_upgrade_exp: Int? = null
        var user_fans:Int? = null
        var user_follow:Int? = null
        var user_video_like:Int? = null
    }
}
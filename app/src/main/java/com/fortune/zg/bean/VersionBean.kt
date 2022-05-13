package com.fortune.zg.bean

class VersionBean {

    companion object {
        private var mDate: DataBean? = null

        fun getData() = mDate
        fun setData(data: DataBean) {
            this.mDate = data
        }
        fun clear(){
            this.mDate = null
        }
    }

    /**
     * code : 1
     * msg : success
     * data : {"version_name":"1.0.0","version_number":20210119,"update_type":2,"update_msg":"更新信息\r\n更新信息","update_url":"http://www.baidu,com"}
     */
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
        /**
         * version_name : 1.0.0
         * version_number : 20210119
         * update_type : 2
         * update_msg : 更新信息
         * 更新信息
         * update_url : http://www.baidu,com
         */
        var version_name: String? = null
        var version_number: Int? = null
        var update_type: Int? = null
        var update_msg: String? = null
        var update_url: String? = null
        var default_page: String? = null
    }
}
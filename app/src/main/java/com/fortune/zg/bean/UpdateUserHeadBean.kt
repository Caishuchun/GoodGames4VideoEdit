package com.fortune.zg.bean

class UpdateUserHeadBean {
    var code = 0
    var msg: String? = null
    var data: Data? = null

    inner class Data {
        var avatar_path: String? = null
    }
}
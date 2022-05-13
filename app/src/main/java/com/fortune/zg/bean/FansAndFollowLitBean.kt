package com.fortune.zg.bean

data class FansAndFollowLitBean(
    val code: Int,
    val `data`: List<Data>,
    val msg: String
) {
    data class Data(
        val user_id: Int,
        val user_name: String
    )
}
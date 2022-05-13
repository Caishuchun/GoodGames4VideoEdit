package com.fortune.zg.bean

data class GetConfigBean(
    val code: Int,
    val msg: String,
    val `data`: Data
) {
    data class Data(
        val qq: String
    )
}
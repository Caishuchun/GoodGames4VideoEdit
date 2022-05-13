package com.fortune.zg.bean

data class IssueVideoBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val cover: String,
        val id: Int,
        val url: String
    )
}
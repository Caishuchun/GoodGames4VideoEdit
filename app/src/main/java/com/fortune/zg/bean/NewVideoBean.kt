package com.fortune.zg.bean

data class NewVideoBean(
    val `data`: Data,
    val message_type: String
) {
    data class Data(
        val platform: String
    )
}
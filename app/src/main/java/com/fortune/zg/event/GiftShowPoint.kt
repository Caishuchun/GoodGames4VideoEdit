package com.fortune.zg.event

data class GiftShowPoint(
    val isShowDailyCheck: GiftShowState,
    val isShowWhitePiao: GiftShowState,
    val isShowInviteGift: GiftShowState
)

enum class GiftShowState {
    SHOW, //显示
    UN_SHOW, //不显示
    USELESS //不用管
}
package com.fortune.zg.bean

class CommonContentBean {
    private var url: String? = null
    private var cover: String? = null
    private var text: String? = null
    private var video_id: Int? = null
    private var list: List<ListBean?>? = null

    fun getUrl(): String? {
        return url
    }

    fun setUrl(url: String?) {
        this.url = url
    }

    fun getCover(): String? {
        return cover
    }

    fun setCover(cover: String?) {
        this.cover = cover
    }

    fun getText(): String? {
        return text
    }

    fun setText(text: String?) {
        this.text = text
    }

    fun getVideo_id(): Int? {
        return video_id
    }

    fun setVideo_id(video_id: Int?) {
        this.video_id = video_id
    }

    fun getList(): List<ListBean?>? {
        return list
    }

    fun setList(list: List<ListBean?>?) {
        this.list = list
    }

    class ListBean {
        var url: String? = null
        var cover: String? = null
        var video_id: Int? = null
    }
}
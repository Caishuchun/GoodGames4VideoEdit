package com.fortune.zg.bean

class RoleInfoBean {

    companion object {
        private var mDate: MutableList<DataBean.BtnBean>? = null

        fun getData() = mDate
        fun setData(data: MutableList<DataBean.BtnBean>) {
            this.mDate = data
        }

        fun clear(){
            this.mDate = null
        }
    }

    /**
     * code : 1
     * msg : success
     * data : {"server_time":1618388439,"role_server_name":"1月份统战区","role_name":"10秒的娘们儿","role_level":1,"role_gender":0,"role_job":0,"role_exp":1,"role_maxexp":5,"role_extra":[{"k":"生命值","v":"19"},{"k":"魔法值","v":"15"},{"k":"当前地图","v":"盟重省"},{"k":"金币","v":"0"},{"k":"元宝","v":"0"},{"k":"金刚石","v":"0"},{"k":"灵符","v":"0"},{"k":"声望","v":"0"}],"btn":[{"btn_id":1,"btn_name":"+10%暴击","btn_rule":{"only_once":0,"interval":360},"click_time":0},{"btn_id":2,"btn_name":"+10%切割","btn_rule":{"only_once":0,"interval":180},"click_time":0},{"btn_id":3,"btn_name":"火龙戒指","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":4,"btn_name":"屠龙刀","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":5,"btn_name":"倚天剑","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":6,"btn_name":"+20%攻击","btn_rule":{"only_once":0,"interval":180},"click_time":0}]}
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
         * server_time : 1618388439
         * role_server_name : 1月份统战区
         * role_name : 10秒的娘们儿
         * role_level : 1
         * role_gender : 0
         * role_job : 0
         * role_exp : 1
         * role_maxexp : 5
         * role_extra : [{"k":"生命值","v":"19"},{"k":"魔法值","v":"15"},{"k":"当前地图","v":"盟重省"},{"k":"金币","v":"0"},{"k":"元宝","v":"0"},{"k":"金刚石","v":"0"},{"k":"灵符","v":"0"},{"k":"声望","v":"0"}]
         * btn : [{"btn_id":1,"btn_name":"+10%暴击","btn_rule":{"only_once":0,"interval":360},"click_time":0},{"btn_id":2,"btn_name":"+10%切割","btn_rule":{"only_once":0,"interval":180},"click_time":0},{"btn_id":3,"btn_name":"火龙戒指","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":4,"btn_name":"屠龙刀","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":5,"btn_name":"倚天剑","btn_rule":{"only_once":1,"interval":0},"click_time":0},{"btn_id":6,"btn_name":"+20%攻击","btn_rule":{"only_once":0,"interval":180},"click_time":0}]
         */
        var server_time: Int = 0
        var role_server_name: String? = null
        var role_name: String? = null
        var role_level: Int = 0
        var role_gender: Int = 0
        var role_job: Int = 0
        var role_exp: String? = null
        var role_maxexp: String? = null
        var role_extra: List<RoleExtraBean>? = null
        var btn: List<BtnBean>? = null

        class RoleExtraBean {
            /**
             * k : 生命值
             * v : 19
             */
            var k: String? = null
            var v: String? = null
        }

        class BtnBean {
            /**
             * btn_id : 1
             * btn_name : +10%暴击
             * btn_rule : {"only_once":0,"interval":360}
             * click_time : 0
             */
            var btn_id: Int = 0
            var btn_name: String? = null
            var btn_rule: BtnRuleBean? = null
            var click_time: Int = 0

            class BtnRuleBean {
                /**
                 * only_once : 0
                 * interval : 360
                 */
                var only_once: Int = 0
                var interval: Int = 0
            }
        }
    }
}
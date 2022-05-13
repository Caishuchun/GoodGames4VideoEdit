package com.fortune.zg.activity

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.bigkoo.pickerview.listener.OnTimeSelectListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.event.UserInfoChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.language.LanguageConfig
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.luck.picture.lib.style.PictureCropParameterStyle
import com.luck.picture.lib.style.PictureParameterStyle
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_user_info.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UserInfoActivity : BaseActivity() {

    private var updateUserHeadObservable: Disposable? = null
    private var updateUserInfoObservable: Disposable? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: UserInfoActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
    }

    private var headPath = ""
    private var path = ""
    private var localPath = ""
    private var name = ""
    private var des = ""
    private var birthday = ""
    private var sex = 0
    private var pathIsChange = false
    private var nameIsChange = false
    private var desIsChange = false
    private var birthdayIsChange = false
    private var sexIsChange = false
    private var canClick = false

    override fun getLayoutId() = R.layout.activity_user_info

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        if (UserInfoBean.getData() != null) {
            val data = UserInfoBean.getData()
            if (!data?.user_avatar.isNullOrEmpty() && !data?.user_avatar!!.endsWith("avatar/default.jpg")) {
                Glide.with(this)
                    .load(data.user_avatar)
                    .placeholder(R.mipmap.head_photo)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(civ_userInfo_head)
                headPath = data.avatar_path!!
            }
            et_userInfo_name.setText(data?.user_name)
            et_userInfo_des.setText(data?.user_desc)
            tv_userInfo_birthday.text =
                if (data?.user_birthday == null) getString(R.string.not_setting) else data.user_birthday
            tv_userInfo_sex.text = when (data?.user_sex) {
                1 -> getString(R.string.boy)
                2 -> getString(R.string.girl)
                else -> getString(R.string.not_setting)
            }
            tv_userInfo_phone.text = data?.user_phone

            path = data?.user_avatar.toString()
            name = data?.user_name.toString()
            des = data?.user_desc.toString()
            birthday = data?.user_birthday.toString()
            sex = data?.user_sex!!
        }

        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_userInfo_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(riv_userInfo_camera)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toGetHead()
            }

        RxTextView.textChanges(et_userInfo_name)
            .skipInitialValue()
            .subscribe {
                nameIsChange = it.toString() != name
                toChangeSaveStatus()
            }
        RxTextView.textChanges(et_userInfo_des)
            .skipInitialValue()
            .subscribe {
                desIsChange = it.toString() != des
                toChangeSaveStatus()
            }
        RxView.clicks(tv_userInfo_birthday)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                OtherUtils.hindKeyboard(this, tv_userInfo_birthday)
                toGetBirthday()
            }
        RxView.clicks(iv_userInfo_birthday)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                OtherUtils.hindKeyboard(this, iv_userInfo_birthday)
                toGetBirthday()
            }

        RxView.clicks(tv_userInfo_sex)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                OtherUtils.hindKeyboard(this, tv_userInfo_sex)
                toGetSex()
            }
        RxView.clicks(iv_userInfo_sex)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                OtherUtils.hindKeyboard(this, iv_userInfo_sex)
                toGetSex()
            }

        RxView.clicks(tv_userInfo_save)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (canClick) {
                    OtherUtils.hindKeyboard(this, tv_userInfo_save)
                    toCheckUserInfo()
                }
            }
    }

    /**
     * 获取头像
     */
    private fun toGetHead() {
        val pictureParameterStyle = PictureParameterStyle()
        pictureParameterStyle.isChangeStatusBarFontColor = true
        pictureParameterStyle.pictureStatusBarColor =
            ContextCompat.getColor(this, R.color.white_FFFFFF)
        pictureParameterStyle.isOpenCheckNumStyle = false
        pictureParameterStyle.isOpenCompletedNumStyle = false
        pictureParameterStyle.pictureTitleTextSize = 20
        pictureParameterStyle.pictureTitleTextColor =
            ContextCompat.getColor(this, R.color.black_1A241F)
        pictureParameterStyle.pictureCancelTextColor =
            ContextCompat.getColor(this, R.color.orange_FFC273)
        pictureParameterStyle.pictureLeftBackIcon = R.mipmap.back_black
        pictureParameterStyle.pictureTitleUpResId = R.mipmap.up
        pictureParameterStyle.pictureTitleDownResId = R.mipmap.down
        pictureParameterStyle.pictureBottomBgColor =
            ContextCompat.getColor(this, R.color.black_2A2C36)
        pictureParameterStyle.picturePreviewTextColor =
            ContextCompat.getColor(this, R.color.green_2EA992)
        pictureParameterStyle.pictureUnPreviewTextColor =
            ContextCompat.getColor(this, R.color.gray_F7F7F7)
        pictureParameterStyle.pictureCompleteTextColor =
            ContextCompat.getColor(this, R.color.green_2EA992)
        pictureParameterStyle.pictureUnCompleteTextColor =
            ContextCompat.getColor(this, R.color.gray_F7F7F7)

        val pictureCropParameterStyle = PictureCropParameterStyle(
            ContextCompat.getColor(this, R.color.white_FFFFFF),
            ContextCompat.getColor(this, R.color.white_FFFFFF),
            ContextCompat.getColor(this, R.color.black_2A2C36),
            pictureParameterStyle.isChangeStatusBarFontColor
        )

        PictureSelector.create(this)
            .openGallery(PictureMimeType.ofImage())
            .isCamera(false)
            .selectionMode(PictureConfig.SINGLE)
            .isSingleDirectReturn(true)
            .loadImageEngine(GlideEngine.createGlideEngine())
            .setPictureStyle(pictureParameterStyle)
            .setPictureCropStyle(pictureCropParameterStyle)
            .enableCrop(true)
            .isCompress(true)
            .circleDimmedLayer(true)
            .cropImageWideHigh(315 * 2, 190 * 2)
            .withAspectRatio(1, 1)
            .freeStyleCropEnabled(true)
            .showCropFrame(false)
            .showCropGrid(false)
            .setLanguage(LanguageConfig.CHINESE)
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: MutableList<LocalMedia>?) {
                    val select = result!![0]
                    pathIsChange = true
                    toChangeSaveStatus()
//                    LogUtils.d("path = ${select.compressPath}")
                    localPath = if (select.isCompressed) {
                        select.compressPath
                    } else {
                        select.cutPath
                    }
                    Glide.with(this@UserInfoActivity)
                        .load(localPath)
                        .placeholder(R.mipmap.head_photo)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(civ_userInfo_head)
                }

                override fun onCancel() {
                }
            })
    }

    /**
     * 静默上传头像
     */
    private fun toUpdateHead(cutPath: String) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        val file = File(cutPath)
        val body = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        builder.addFormDataPart("image", URLEncoder.encode(file.name, "UTF-8"), body)
        val parts = builder.build().parts()
        val updateUserHead = RetrofitUtils.builder().updateUserHead(parts[0])
        updateUserHeadObservable = updateUserHead.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            headPath = it.data?.avatar_path.toString()
                            toUpdateUserInfo()
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            Glide.with(this)
                                .load(UserInfoBean.getData()!!.user_avatar)
                                .placeholder(R.mipmap.head_photo)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(civ_userInfo_head)
                            DialogUtils.dismissLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 检查用户信息用户信息
     */
    private fun toCheckUserInfo() {
        if (pathIsChange) {
            DialogUtils.showBeautifulDialog(this)
            toUpdateHead(localPath)
        } else {
            when {
                et_userInfo_name.text.isEmpty() -> {
                    ToastUtils.show(getString(R.string.string_017))
                }
                OtherUtils.isGotOutOfLine(this, et_userInfo_name.text.toString()) == 1 -> {
                    ToastUtils.show("昵称" + getString(R.string.string_057))
                }
                OtherUtils.isGotOutOfLine(this, et_userInfo_name.text.toString()) == 2 -> {
                    ToastUtils.show("昵称" + getString(R.string.string_053))
                }
                OtherUtils.isGotOutOfLine(this, et_userInfo_des.text.toString()) == 1 -> {
                    ToastUtils.show("个人简介" + getString(R.string.string_057))
                }
                OtherUtils.isGotOutOfLine(this, et_userInfo_des.text.toString()) == 2 -> {
                    ToastUtils.show("个人简介" + getString(R.string.string_053))
                }
                else -> {
                    DialogUtils.showBeautifulDialog(this)
                    toUpdateUserInfo()
                }
            }
        }
    }

    /**
     * 正式更新数据
     */
    @SuppressLint("CheckResult")
    private fun toUpdateUserInfo() {
        val currentName = et_userInfo_name.text.toString().trim()
        val currentDes = et_userInfo_des.text.toString().trim()
        val currentBirthday = tv_userInfo_birthday.text.toString().trim()
        val currentSex = when (tv_userInfo_sex.text.toString().trim()) {
            getString(R.string.boy) -> 1
            getString(R.string.girl) -> 2
            else -> 0
        }
        val updateUserInfo = RetrofitUtils.builder().updateUserInfo(
            currentName, currentDes, currentSex, currentBirthday, headPath
        )
        updateUserInfoObservable = updateUserInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val data = UserInfoBean.getData()!!
                            data.user_avatar = "http://cdn.5745.com/avatar/$headPath"
                            data.user_name = currentName
                            data.user_desc = currentDes
                            data.user_birthday = currentBirthday
                            data.user_sex = currentSex
                            UserInfoBean.setData(data)
                            EventBus.getDefault()
                                .postSticky(
                                    UserInfoChange(
                                        "http://cdn.5745.com/avatar/$headPath",
                                        currentName,
                                        currentDes
                                    )
                                )
                            ToastUtils.show(getString(R.string.update_success))
                            finish()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 获取性别
     */
    private fun toGetSex() {
        BottomDialog.showSelectDialog(
            this,
            R.array.boy_or_girl,
            object : BottomDialog.OnBottomDialog4MustListener {
                override fun index(index: Int) {
                    sexIsChange = (index + 1) != sex
                    toChangeSaveStatus()
                }

                override fun select(data: String) {
                    tv_userInfo_sex.text = data
                }
            })
    }

    /**
     * 获取生日
     */
    @SuppressLint("SimpleDateFormat")
    private fun toGetBirthday() {
        val df = SimpleDateFormat("yyyy-MM-dd")
        TimePickerBuilder(this, OnTimeSelectListener { date, v ->
            tv_userInfo_birthday.text = df.format(Date(date.time))
            birthdayIsChange = tv_userInfo_birthday.text.toString().trim() != birthday
            toChangeSaveStatus()
        }).setSubmitColor(resources.getColor(R.color.green_2EA992))
            .setCancelColor(resources.getColor(R.color.orange_FFC273))
            .setCancelText(getString(R.string.cancel))
            .build().show()
    }

    /**
     * 修改保存键状态
     */
    private fun toChangeSaveStatus() {
        if (pathIsChange || nameIsChange || desIsChange || birthdayIsChange || sexIsChange) {
            canClick = true
            tv_userInfo_save.setTextColor(resources.getColor(R.color.green_56A793))
        } else {
            canClick = false
            tv_userInfo_save.setTextColor(resources.getColor(R.color.green_5963C5AD))
        }
    }

    override fun destroy() {
        updateUserHeadObservable?.dispose()
        updateUserInfoObservable?.dispose()

        updateUserInfoObservable = null
        updateUserHeadObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}
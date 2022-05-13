package com.fortune.zg.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Copyright (C)
 * FileName: BaseFragment
 * Author: wudengwei
 * Date: 2018/12/17 20:15
 * Description: 解决ViewPager中使用显示与隐藏和onResume()与onPause()不一致问题
 */
public class BaseFragment extends Fragment {
    protected Activity mActivity;
    //是否第一次可见
    private boolean isFirstVisible = true;
    //是否可见
    private boolean isVisible = false;
    /**
     * onResume是否已经执行,fragment在ViewPager中
     * 第一次创建时(预加载)不管可不可见，都会执行一次生命周期
     * isFirstOnResume=false表示预加载或当前显示的fragment执行了onResume()
     * isFirstOnResume=false，fragment再次显示时，当做执行了onResume()
     */
    private boolean isFirstOnResume = true;

    /**
     * 作用于onResume()在显示状态下执行后，fragment隐藏时执行对应的onPause()
     * 避免其他页面的onPause()执行
     * 是否可以使用isVisible代替？
     */
    private boolean isOnResumeVisible = false;

    //标识显示或隐藏的来源
    public final int tagOnResume = 1;
    public final int tagVisibleToUser = 2;
    public final int tagOnPause = 3;

    @Override
    public void onAttach(Context context) {
        this.mActivity = (Activity) context;
        super.onAttach(context);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() == null)
            return;
        /*可见回调方法*/
        if (isVisible != isVisibleToUser)
            onVisibleChange(isVisibleToUser, isFirstVisible);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFirstVisible = true;
        isVisible = false;
        isFirstOnResume = true;
        isOnResumeVisible = false;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getUserVisibleHint()) {
            /*可见,是否第一次*/
            onVisibleChange(true, isFirstVisible);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstOnResume) {
            isFirstOnResume = false;
        }
        if (getUserVisibleHint() && !isOnResumeVisible) {
            //onResume()在可见状态下调用
            isOnResumeVisible = true;
            onResumeVisible(tagOnResume);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //onResume()在可见状态下调用后，进入onPause()
        if (!isFirstOnResume && isOnResumeVisible) {
            isOnResumeVisible = false;
            onPauseInVisible(tagOnPause);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFirstVisible = true;
        isVisible = false;
        isFirstOnResume = true;
    }

    /*是否可见回调方法*/
    protected void onVisibleChange(boolean isVisible, boolean firstVisible) {
        this.isVisible = isVisible;
        if (firstVisible) {
            onLazyLoad();
            isFirstVisible = false;
        }
        if (isVisible && !isFirstOnResume && !isOnResumeVisible) {
            //isFirstOnResume=false表示fragment预加载，onResume()在可见状态下调用
            isOnResumeVisible = true;
            onResumeVisible(tagVisibleToUser);
        }
        if (!isVisible && !isFirstOnResume && isOnResumeVisible) {
            isOnResumeVisible = false;
            onPauseInVisible(tagVisibleToUser);
        }
    }

    /**
     * 懒加载（第一次可见）
     */
    protected void onLazyLoad() {

    }

    /**
     * 替代具有不确定性的onResume()（ViewPager可能会让多个fragment执行onResume()，但有些fragment执行后是不可见状态的）
     * 我需要的是执行onResume后，确定是可见状态的方法
     *
     * @param tag 标识显示的来源
     */
    protected void onResumeVisible(int tag) {
    }

    /**
     * 替代具有不确定性的onPause()（ViewPager可能会让多个fragment执行onPause()）
     * 我需要的是执行onPause前，确定执行onResume()
     *
     * @param tag 标识隐藏的来源
     */
    protected void onPauseInVisible(int tag) {
        isFirstVisible = true;
    }
}
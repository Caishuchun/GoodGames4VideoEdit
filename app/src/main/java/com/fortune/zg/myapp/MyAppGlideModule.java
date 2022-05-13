package com.fortune.zg.myapp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Author: 蔡小树
 * Time: 2020/1/8 16:06
 * Description:
 */

@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        RequestOptions requestOptions = new RequestOptions()
                .skipMemoryCache(true) //不使用内存缓存
                .diskCacheStrategy(DiskCacheStrategy.NONE);  //原图和缩略图都不进行磁盘缓存
        builder.setDefaultRequestOptions(requestOptions);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
    }
}

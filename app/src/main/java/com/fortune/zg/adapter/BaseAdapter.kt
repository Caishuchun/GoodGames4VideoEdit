package com.fortune.zg.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Author: 蔡小树
 * Time: 2019/12/25 18:14
 * Description:
 */

class BaseAdapter<T> private constructor() : RecyclerView.Adapter<BaseAdapter<T>.BaseViewHolder>() {
    private var mDataList: List<T>? = null
    private var mLayoutId: Int? = null
    private var addBindView: ((itemView: View, itemData: T) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(mLayoutId!!, parent, false)
        return BaseViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDataList!!.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        addBindView!!.invoke(holder.itemView, mDataList!![position])
    }

    inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class Builder<B> {
        private var baseAdapter: BaseAdapter<B> = BaseAdapter()

        fun setData(lists: List<B>): Builder<B> {
            baseAdapter.mDataList = lists
            return this
        }

        fun setLayoutId(layoutId: Int): Builder<B> {
            baseAdapter.mLayoutId = layoutId
            return this
        }

        fun addBindView(itemBind: ((itemView: View, itemData: B) -> Unit)): Builder<B> {
            baseAdapter.addBindView = itemBind
            return this
        }

        fun create(): BaseAdapter<B> {
            return baseAdapter
        }
    }
}
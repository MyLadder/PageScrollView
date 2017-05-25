package com.rexy.widget;

import android.view.View;
import android.view.ViewGroup;

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2016-01-06 10:20
 */
public interface ItemProvider {
    CharSequence getTitle(int position);

    Object getItem(int position);

    int getCount();

    interface ViewProvider extends ItemProvider {
        int getViewType(int position);

        View getView(int position, View convertView, ViewGroup parent);
    }
}



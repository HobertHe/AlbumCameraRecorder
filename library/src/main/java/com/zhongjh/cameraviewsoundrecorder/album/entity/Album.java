/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhongjh.cameraviewsoundrecorder.album.entity;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.zhongjh.cameraviewsoundrecorder.R;
import com.zhongjh.cameraviewsoundrecorder.album.loader.AlbumLoader;

/**
 * 专辑
 */
public class Album implements Parcelable {
    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Nullable
        @Override
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
    public static final String ALBUM_ID_ALL = String.valueOf(-1);
    public static final String ALBUM_NAME_ALL = "All";

    private final String mId;
    private final String mCoverPath;
    private final String mDisplayName;
    private long mCount;

    Album(String id, String coverPath, String albumName, long count) {
        mId = id;
        mCoverPath = coverPath;
        mDisplayName = albumName;
        mCount = count;
    }

    Album(Parcel source) {
        mId = source.readString();
        mCoverPath = source.readString();
        mDisplayName = source.readString();
        mCount = source.readLong();
    }

    /**
     * {@link Cursor} 构建一个新的实体 {@link Album}
     * 此方法不负责管理光标资源，如关闭、迭代等。
     */
    public static Album valueOf(Cursor cursor) {
        return new Album(
                cursor.getString(cursor.getColumnIndex("bucket_id")),
                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)),
                cursor.getString(cursor.getColumnIndex("bucket_display_name")),
                cursor.getLong(cursor.getColumnIndex(AlbumLoader.COLUMN_COUNT)));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mCoverPath);
        dest.writeString(mDisplayName);
        dest.writeLong(mCount);
    }

    public String getId() {
        return mId;
    }

    public String getCoverPath() {
        return mCoverPath;
    }

    public long getCount() {
        return mCount;
    }

    public void addCaptureCount() {
        mCount++;
    }

    /**
     * 显示名称，可能返回“全部”
     * @return 返回名称
     */
    public String getDisplayName(Context context) {
        if (isAll()) {
            return context.getString(R.string.album_name_all);
        }
        return mDisplayName;
    }

    /**
     * 判断如果id = -1的话，就是查询全部的意思
     * @return 是否全部
     */
    public boolean isAll() {
        return ALBUM_ID_ALL.equals(mId);
    }

    public boolean isEmpty() {
        return mCount == 0;
    }

}
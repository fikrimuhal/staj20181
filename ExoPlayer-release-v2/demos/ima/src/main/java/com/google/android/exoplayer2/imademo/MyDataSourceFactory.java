package com.google.android.exoplayer2.imademo;

import com.google.android.exoplayer2.upstream.DataSource;

public class MyDataSourceFactory implements DataSource.Factory {
    @Override
    public DataSource createDataSource() {
        return new MyDataSource();
    }
}

package com.hivecdn.androidp2p;

import com.google.android.exoplayer2.upstream.DataSource;

public class P2PDataSourceFactory implements DataSource.Factory {
    @Override
    public DataSource createDataSource() {
        return new P2PDataSource();
    }
}

package com.google.android.exoplayer2.imademo;

import android.content.res.AssetManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;

public class MyDataSource implements DataSource {

    DataSpec dataSpec;
    InputStream is;

    @Override
    public long open(DataSpec _dataSpec) throws IOException {
        dataSpec = _dataSpec;
        AssetManager mngr = MainActivity.mngr;
        assert(dataSpec.uri.equals("demo.mp4"));
        Log.v("mylog", "open now!");
        try {
            is = mngr.open("demo.mp4");
        }
        catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        is.skip(dataSpec.position);
        return is.available();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return is.read(buffer, offset, readLength);
    }

    @Nullable
    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}

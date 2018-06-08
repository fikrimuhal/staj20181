package com.hivecdn.androidp2p;

import android.content.res.AssetManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import scala.NotImplementedError;

public class P2PDataSource implements DataSource, VideoPeerConnection.MyInterface {

    private final String TAG = P2PDataSource.class.getName();

    private class Triad implements Comparator<Triad>{
        @Override
        public int compare(Triad x, Triad y) {
            return y.start-x.start;
        }

        public ByteBuffer buf;
        public int start, len;

        Triad(ByteBuffer _buf, int _start, int _len) {
            buf = _buf;
            start = _start;
            len = _len;
        }

    };

    DataSpec dataSpec;
    VideoPeerConnection vpc;
    int pos = 0;
    TreeSet<Triad> triads;

    P2PDataSource() {
        triads = new TreeSet<Triad>();
    }

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onRequest(int start, int len) {
        return ; // Do nothing. We're the receiver, we have no data.
        // TODO: In a real application, there must be no 'receiver' or 'transmitter'. All sides must keep chunks of video in memory and respond to all requests they can.
    }

    @Override
    public void onResponse(ByteBuffer buf, int start, int len) {
        synchronized (triads) {
            triads.add(new Triad(buf.slice(), start, len));
            triads.notifyAll();
        }
    }

    @Override
    public void onIdReceived(String ourId) {

    }

    @Override
    public void onConnected(String otherId) {
        vpc.requestRange((int)dataSpec.position, (int)dataSpec.length);
    }

    @Override
    public long open(DataSpec _dataSpec) throws IOException {
        dataSpec = _dataSpec;
        pos = (int)dataSpec.position;
        synchronized (triads) {
            triads.clear();
        }
        vpc = new VideoPeerConnection(MainActivity.context, dataSpec.uri.toString(), this);
        return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Triad bestMatch;
        synchronized (triads) {
            while (true) {
                bestMatch = triads.floor(new Triad(null, pos, 0));
                if (bestMatch != null && bestMatch.start+bestMatch.len > pos)
                    break;
                try {
                    Log.v(TAG, "read found no suitable triads. Waiting...");
                    triads.wait();
                }
                catch (InterruptedException e) {
                }
            }
        }
        final int readLength2 = Math.min(bestMatch.start + bestMatch.len, pos+readLength) - pos;
        for (int i=0;i<readLength2;i++)
            buffer[i+offset] = bestMatch.buf.get(pos-bestMatch.start);
        if (readLength >= readLength2) {// If all of the current triad has been read, we can delete it from memory.
            synchronized (triads) {
                triads.remove(bestMatch);
            }
        }
        pos += readLength2;
        return readLength2;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        vpc.close();
    }
}

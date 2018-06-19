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

import static com.google.android.exoplayer2.C.LENGTH_UNSET;

public class P2PDataSource implements DataSource, VideoPeerConnection.MyInterface, SignalingServerConnection.SignalingListener {

    private final String TAG = P2PDataSource.class.getName();

    private class Triad implements Comparable<Triad>{
        @Override
        public int compareTo(Triad other) {
            return other.start-start;
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
    SignalingServerConnection ssc;
    int pos = 0;
    TreeSet<Triad> triads;

    P2PDataSource() {
        Log.v(TAG, "New P2PDataSource");
        triads = new TreeSet<Triad>();
    }

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(VideoPeerConnection _vpc) {
        if (vpc != null) {
            _vpc.close();
            return ;
        }
        vpc = _vpc;
        Log.v(TAG, "Requesting range [" + dataSpec.position + "," + (dataSpec.position+dataSpec.length) + ")");
        vpc.requestRange((int)dataSpec.position, (int)dataSpec.length);
    }

    @Override
    public void onPeerDisconnected(VideoPeerConnection _vpc) {
        if (vpc == _vpc)
            vpc = null;
    }

    @Override
    public void onRequest(VideoPeerConnection _vpc, int start, int len) {
        return ; // Do nothing. We're the receiver, we have no data.
        // TODO: In a real application, there must be no 'receiver' or 'transmitter'. All sides must keep chunks of video in memory and respond to all requests they can.
    }

    @Override
    public void onResponse(VideoPeerConnection _vpc, ByteBuffer buf, int start, int len) {
        if (vpc != _vpc)
            return ;
        Log.v(TAG, "Received range [" + start + "," + (start+len) + ")");
        synchronized (triads) {
            triads.add(new Triad(buf.slice(), start, len));
            Log.v(TAG, "Size of the set after the addition: " + triads.size());
            triads.notifyAll();
        }
    }

    @Override
    public long open(DataSpec _dataSpec) throws IOException {
        dataSpec = _dataSpec;
        pos = (int)dataSpec.position;
        Log.v(TAG, "open called. Clearing triads... (pos: " + pos + ")");
        synchronized (triads) {
            triads.clear();
        }
        if (ssc == null)
            ssc = new SignalingServerConnection(MainActivity.context, this, this, dataSpec.uri.toString());
        else if (vpc != null) {
            Log.v(TAG, "Requesting range [" + dataSpec.position + "," + (dataSpec.position+dataSpec.length) + ")");
            vpc.requestRange((int)dataSpec.position, (int)dataSpec.length);
        }
        else
            Log.v(TAG,"Whoops");
        //vpc = new VideoPeerConnection(MainActivity.context, dataSpec.uri.toString(), this);
        return LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Log.v(TAG, "read: pos: " + pos + "readLength: " + readLength);
        Triad bestMatch;
        synchronized (triads) {
            while (true) {
                bestMatch = triads.ceiling(new Triad(null, pos, 0)); // Java documentation seems to be off here. Floor find the lower bound, ceiling finds the upper bound, but this doesn't make sense, nor is this how TreeSet is documented.
                if (bestMatch != null && bestMatch.start+bestMatch.len > pos) {
                    //Log.v(TAG, "Suitable triad found.");
                    break;
                }
                try {
                    Log.v(TAG, "read found no suitable triads. Waiting... (pos: " + pos + ") bestMatch = " + (bestMatch==null?"null":"sth") + ", set size: " + triads.size());
                    triads.wait();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        final int readLength2 = Math.min(bestMatch.start + bestMatch.len, pos+readLength) - pos;
        for (int i=0;i<readLength2;i++) {
            buffer[i + offset] = bestMatch.buf.get(pos+i - bestMatch.start);
            //if (pos+i == 3)
               // Log.d(TAG, "Byte " + (pos + i) + " is: " + buffer[i + offset]);
        }
        if (readLength > readLength2) {// If all of the current triad has been read, we can delete it from memory.
            synchronized (triads) {
                Log.v(TAG, "Removing the current triad, as it has been exhausted. (readLength: " + readLength + ", readLength2: " + readLength2 + ")");
                triads.remove(bestMatch);
            }
        }
        pos += readLength2;
        //Log.v(TAG, "read() returns");
        return readLength2;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        Log.v(TAG, "closing");
        if (vpc != null) {
            vpc.close();
            vpc = null;
        }
        if (ssc != null) {
            ssc.close();
            ssc = null;
        }
    }
}

package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;

import static java.lang.Math.min;

public class BenchSenderActivity extends AppCompatActivity implements VideoPeerConnection.MyInterface{

    final String TAG = BenchSenderActivity.class.getName();

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onRequest(int start, int len) {
        while (len > 0) {
            final int lenThisTime = min(1024*1024, len);
            byte[] buf = new byte[lenThisTime];
            for (int i=0;i<lenThisTime; i++)
                buf[i] = (byte)(start+i);
            Log.v(TAG, "Sending range [" + start + ", " + (start+lenThisTime) + ")");
            vpc.sendRange(buf, start, lenThisTime);
            len -= lenThisTime;
        }
    }

    @Override
    public void onResponse(ByteBuffer buf, int start, int len) {
        ; // Don't do anything. We're the sender.
    }

    @Override
    public void onIdReceived(String ourId) {
        Log.v(TAG, "Our id is: " + ourId);
    }

    @Override
    public void onConnected(String otherId) {
        Log.v(TAG, "Connected to peer id: " + otherId);
    }

    VideoPeerConnection vpc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bench_sender);
        vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video.mp4", this);
    }
}

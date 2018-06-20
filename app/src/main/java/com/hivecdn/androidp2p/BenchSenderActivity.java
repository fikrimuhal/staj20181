package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;

import static java.lang.Math.min;

public class BenchSenderActivity extends AppCompatActivity implements VideoPeerConnection.VideoPeerConnectionListener, SignalingServerConnection.SignalingListener{

    final String TAG = BenchSenderActivity.class.getName();

    SignalingServerConnection ssc;
    VideoPeerConnectionFactory vpcFactory;

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(WebRtcPeerConnection _vpc) {
        // We don't do anything at this point. The sender waits for the receiver to receive a range.
    }

    @Override
    public void onPeerDisconnected(WebRtcPeerConnection _vpc) {
    }

    @Override
    public void onRequest(VideoPeerConnection vpc, int start, int len) {
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
    public void onResponse(VideoPeerConnection vpc, ByteBuffer buf, int start, int len) {
        ; // Don't do anything. We're the sender.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bench_sender);
        vpcFactory = new VideoPeerConnectionFactory(this);
        ssc = new SignalingServerConnection(MainActivity.context, vpcFactory, this, "http://www.hivecdn.com/benchmark/video2.mp4");
        //vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video2.mp4", this);
    }
}

package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

import static java.lang.Math.min;

public class BenchSenderActivity extends AppCompatActivity implements SignalingServerConnection.SignalingListener, BinaryWebRtcPeerConnection.BinaryWebRtcListenerInterface {

    final String TAG = BenchSenderActivity.class.getName();

    SignalingServerConnection ssc;
    BinaryWebRtcPeerConnectionFactory pcFactory;

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(WebRtcPeerConnection _pc) {
        Log.v(TAG, "New peer connected."); // Wait for the peer to send a message before sending it the benchmark payload.
    }

    @Override
    public void onMessage(WebRtcPeerConnection pc, DataChannel.Buffer _buffer) {
        int bytesLeft = BenchReceiverActivity.BenchDataSize;
        ByteBuffer buffer = ByteBuffer.allocate(16*1024);
        while (bytesLeft > 0) {
            final int thisTimeByteCnt = min(16*1024, bytesLeft);
            buffer.position(0);
            buffer.limit(thisTimeByteCnt);
            pc.sendMessage(new DataChannel.Buffer(buffer, true));
            bytesLeft -= thisTimeByteCnt;
        }
    }

    @Override
    public void onPeerDisconnected(WebRtcPeerConnection _vpc) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bench_sender);
        pcFactory = new BinaryWebRtcPeerConnectionFactory(this);
        ssc = new SignalingServerConnection(MainActivity.context, pcFactory, this, "http://www.hivecdn.com/benchmark/video2.mp4");
        //vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video2.mp4", this);
    }
}

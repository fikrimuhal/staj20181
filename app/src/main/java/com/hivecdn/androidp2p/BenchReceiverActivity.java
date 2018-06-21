package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;
import java.util.Calendar;

public class BenchReceiverActivity extends AppCompatActivity implements View.OnClickListener, SignalingServerConnection.SignalingListener, BinaryWebRtcPeerConnection.BinaryWebRtcListenerInterface {

    final String TAG = BenchReceiverActivity.class.getName();

    final long MinDisplayUpdateInterval = 250;
    public static final int BenchDataSize = 10*1024*1024; // 10mb

    SignalingServerConnection ssc;
    BinaryWebRtcPeerConnectionFactory pcFactory;
    BinaryWebRtcPeerConnection pc;
    EditText editText;
    TextView logView;
    Button goButton;
    int numBytesLeft;
    long startTime;
    long lastDisplayUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bench_receiver);
        editText = findViewById(R.id.editText);
        logView = findViewById(R.id.logView);
        goButton = findViewById(R.id.goButton);
        goButton.setOnClickListener(this);
        goButton.setEnabled(false);
        pcFactory = new BinaryWebRtcPeerConnectionFactory(this);
        ssc = new SignalingServerConnection(MainActivity.context, pcFactory, this, "http://www.hivecdn.com/benchmark/video2.mp4");
        //vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video2.mp4", this);
    }

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(WebRtcPeerConnection _pc) {
        Log.v(TAG, "Connected to peer id: " + _pc.otherPeerId);
        if (pc != null) { // Hope that the first peer we connect to will be a sender. TODO: Fix this.
            _pc.close(); // TODO: Instead of closing connections after establishing them, we should reject before handshake starts.
            Log.v(TAG, "We're already connected to another peer. Ignoring.");
            return ;
        }
        pc = (BinaryWebRtcPeerConnection) _pc;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startRound();
                //goButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onPeerDisconnected(WebRtcPeerConnection _pc) {
        Log.v(TAG, "onPeerDisconnected");
        if (pc == _pc) {
            Log.v(TAG, "This was the peer we're benchmarking with. Setting pc=null.");
            pc = null;
        }
    }

    public void onMessage(WebRtcPeerConnection _pc, DataChannel.Buffer buffer) {
        if (pc != pc)
            return;
        long curTime = Calendar.getInstance().getTimeInMillis();
        numBytesLeft -= buffer.data.remaining();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (numBytesLeft == 0) {
                    //editText.setEnabled(true);
                    //goButton.setEnabled(true);
                    //logView.append("Took " + (curTime-startTime) + " ms.\n");
                    logView.append("Result: " + 8.0*1000*BenchDataSize/1024/1024/(curTime-startTime) + " megabit/sn.\n");
                    startRound();
                }
                else {
                    if (curTime - lastDisplayUpdateTime > MinDisplayUpdateInterval) {
                        editText.setText(String.valueOf(numBytesLeft));
                        lastDisplayUpdateTime = curTime;
                    }
                }
            }
        });
    }

    void startRound() {
        numBytesLeft = BenchDataSize;
        editText.setText(String.valueOf(numBytesLeft));
        startTime = Calendar.getInstance().getTimeInMillis();
        lastDisplayUpdateTime = startTime;
        pc.sendMessage(new DataChannel.Buffer(ByteBuffer.allocate(1), true));
    }

    @Override
    public void onClick(View v) {
    }
}

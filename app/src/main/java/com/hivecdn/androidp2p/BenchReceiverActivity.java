package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class BenchReceiverActivity extends AppCompatActivity implements VideoPeerConnection.MyInterface, View.OnClickListener, SignalingServerConnection.SignalingListener{

    final String TAG = BenchReceiverActivity.class.getName();

    final long MinDisplayUpdateInterval = 250;
    final int BenchDataSize = 10*1024*1024; // 10mb

    SignalingServerConnection ssc;
    VideoPeerConnection vpc;
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
        ssc = new SignalingServerConnection(MainActivity.context, this, this, "http://www.hivecdn.com/benchmark/video2.mp4");
        //vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video2.mp4", this);
    }

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(VideoPeerConnection _vpc) {
        if (vpc != null) { // Hope that the first peer we connect to will be a sender. TODO: Fix this.
            _vpc.close(); // TODO: Instead of closing connections after establishing them, we should reject before handshake starts.
            return ;
        }
        vpc = _vpc;
        Log.v(TAG, "Connected to peer id: " + vpc.otherPeerId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startRound();
                //goButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onPeerDisconnected(VideoPeerConnection _vpc) {
        if (vpc == _vpc)
            vpc = null;
    }

    @Override
    public void onRequest(VideoPeerConnection _vpc, int start, int len) {
        ; // Do nothing. We're the sender.
    }

    @Override
    public void onResponse(VideoPeerConnection _vpc, ByteBuffer buf, int start, int len) {
        if (vpc != _vpc)
            return ;
        Log.v(TAG, "Received range [" + start + ", " + (start+len) + ")");
        for (int i=start, j=0; i<len; i++, j++) {
            if (buf.get(j) != (byte)i)
            {
                Log.v(TAG, "Corrupt payload detected at byte " + i);
                vpc.close();
            }
        }
        long curTime = Calendar.getInstance().getTimeInMillis();
        numBytesLeft -= len;
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
        vpc.requestRange(0, numBytesLeft);
    }

    @Override
    public void onClick(View v) {
        /*if (v == goButton) {
            numBytesLeft = Integer.valueOf(editText.getText().toString());
            if (numBytesLeft > 0) {
                goButton.setEnabled(false);
                editText.setEnabled(false);
                startTime = Calendar.getInstance().getTimeInMillis();
                lastDisplayUpdateTime = startTime;
                vpc.requestRange(0, numBytesLeft);
                Log.v(TAG, "Benchmark started");
            }
        }*/
    }
}

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

public class BenchReceiverActivity extends AppCompatActivity implements VideoPeerConnection.MyInterface, View.OnClickListener{

    final String TAG = BenchReceiverActivity.class.getName();

    final long MinDisplayUpdateInterval = 250;

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
        vpc = new VideoPeerConnection(MainActivity.context, "http://www.hivecdn.com/benchmark/video.mp4", this);
    }

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onRequest(int start, int len) {
        ; // Do nothing. We're the sender.
    }

    @Override
    public void onResponse(ByteBuffer buf, int start, int len) {
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
                    logView.append("Took " + (curTime-startTime) + " ms.\n");
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

    @Override
    public void onIdReceived(String ourId) {
        Log.v(TAG, "Our id is: " + ourId);
    }

    void startRound() {
        numBytesLeft = 10000000; // 10 mb
        editText.setText(String.valueOf(numBytesLeft));
        startTime = Calendar.getInstance().getTimeInMillis();
        lastDisplayUpdateTime = startTime;
        vpc.requestRange(0, numBytesLeft);
    }

    @Override
    public void onConnected(String otherId) {
        Log.v(TAG, "Connected to peer id: " + otherId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startRound();
                //goButton.setEnabled(true);
            }
        });
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

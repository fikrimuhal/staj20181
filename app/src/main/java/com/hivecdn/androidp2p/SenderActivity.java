package com.hivecdn.androidp2p;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.foo.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.min;
import static java.lang.Thread.interrupted;

public class SenderActivity extends AppCompatActivity implements VideoPeerConnection.MyInterface, SignalingServerConnection.SignalingListener {

    final static String TAG = SenderActivity.class.getName();

    static public AssetManager mngr;

    SignalingServerConnection ssc;
    Map<String, Thread> threads;

    @Override
    public void onIdReceived(String ourPeerId, int ourSessionId) {
        // Cool
    }

    @Override
    public void onNewPeer(VideoPeerConnection vpc) {
        // We wait for the receiver to request ranges.
    }

    @Override
    public void onPeerDisconnected(VideoPeerConnection vpc) {
        threads.get(vpc.signalId).interrupt();
        threads.remove(vpc.signalId);
    }

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    public static void onRequestRunner(VideoPeerConnection vpc, int start, int len) {
        Log.v(TAG, "Got request [" + start + "," + (start+len) + ")");
        try {
            InputStream is = mngr.open("demo.mp4");
            is.skip(start);
            if (len < 0)
                len = is.available();
            while (len > 0) {
                if (interrupted())
                    break;
                final int thisTimeLen = min(len, 16*1024);
                byte[] buf = new byte[thisTimeLen];
                int res = is.read(buf, 0, thisTimeLen);
                Log.v(TAG, "Sending response [" + start + "," + (start + res) + ")");
                vpc.sendRange(buf, start, res);
                start += res;
                len -= res;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequest(final VideoPeerConnection vpc, final int start, final int len) {
        if (threads.get(vpc.signalId) != null) {
            threads.get(vpc.signalId).interrupt();
            threads.remove(vpc.signalId);
        }
        Thread thread = new Thread(){
            @Override
            public void run() {
                onRequestRunner(vpc, start, len);
            }
        };

        threads.put(vpc.signalId, thread);
        thread.start();
    }

    @Override
    public void onResponse(VideoPeerConnection vpc, ByteBuffer buf, int start, int len) {
        return ; // Sender only sends.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        threads = new HashMap<>();
        ssc = new SignalingServerConnection(MainActivity.context, this, this, MainActivity.context.getString(R.string.content_url));
        //vpc = new VideoPeerConnection(MainActivity.context, MainActivity.context.getString(R.string.content_url), this);
        mngr = getAssets();
    }
}

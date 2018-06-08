package com.hivecdn.androidp2p;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.foo.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SenderActivity extends AppCompatActivity implements VideoPeerConnection.MyInterface {

    final String TAG = SenderActivity.class.getName();

    static public AssetManager mngr;

    @Override
    public void onVerbose(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onRequest(int start, int len) {
        Log.v(TAG, "Got request [" + start + "," + (start+len) + ")");
        try {
            InputStream is = mngr.open("demo.mp4");
            is.skip(start);
            if (len < 0)
                len = is.available();
            byte[] buf = new byte[len];
            int res = is.read(buf, 0, len);
            /*int debugRangeStart = Math.max(4080, start);
            int debugRangeEnd = Math.min(4100, start+len);
            for (int i=debugRangeStart; i<debugRangeEnd; i++)*/
            for (int i=start; i<2048; i++)
                Log.d(TAG, "Byte " + i + " is: " + buf[i-start]);
            Log.v(TAG, "Sending response [" + start + "," + (start+res) + ")");
            vpc.sendRange(buf, start, res);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResponse(ByteBuffer buf, int start, int len) {
        return ; // Sender only sends.
    }

    @Override
    public void onIdReceived(String ourId) {
        Log.v(TAG, "Our id is: " + ourId);
    }

    @Override
    public void onConnected(String otherId) {
        Log.v(TAG, "Connected to id: " + otherId);
    }

    VideoPeerConnection vpc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        vpc = new VideoPeerConnection(MainActivity.context, MainActivity.context.getString(R.string.content_url), this);
        mngr = getAssets();
    }
}

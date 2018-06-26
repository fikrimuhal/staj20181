package com.hivecdn.androidsqlitedatabase;

import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Random;

import static com.hivecdn.androidsqlitedatabase.StorageHelper.getAvailableExternalMemorySize;
import static com.hivecdn.androidsqlitedatabase.StorageHelper.getAvailableInternalMemorySize;
import static com.hivecdn.androidsqlitedatabase.StorageHelper.getTotalExternalMemorySize;
import static com.hivecdn.androidsqlitedatabase.StorageHelper.getTotalInternalMemorySize;

public class MainActivity extends AppCompatActivity {

    private static final String TAGG = MainActivity.class.getName();

    TextView textView;

    private void databaseBenchmark() {
        DatabaseHelper helper = new DatabaseHelper(this);
        Random random = new Random();
        for (int i=0;i<2000;i++) {
            long start, end;
            byte[] bytes = new byte[512*1024];
            random.nextBytes(bytes);
            start = System.nanoTime();
            long id = helper.insertNote(bytes);
            end = System.nanoTime();
            Log.v(TAGG, String.valueOf(i) + " " + (end-start) + " " + id + "\n");
        }
    }

    private void initBenchmark() {
        long start, end;
        start = System.nanoTime();
        DatabaseHelper helper = new DatabaseHelper(this);
        end = System.nanoTime();
        Log.v(TAGG, "Initialization took " + (end-start) + " ns.");
        for (int id = 1365; id<1500; id++) {
            start = System.nanoTime();
            byte[] bytes = helper.getNote(1365).getNote();
            end = System.nanoTime();
            Log.v(TAGG, "id: " + id + " took " + (end-start) + " ns.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textView.setText(getAvailableInternalMemorySize() + " " + getTotalInternalMemorySize() + " " + getAvailableExternalMemorySize() + " " + getTotalExternalMemorySize());
        //DatabaseHelper helper = new DatabaseHelper(this);
        //long id = helper.insertNote("abc".getBytes());
        //textView.setText("umm:"+ new String(helper.getNote(id).getNote()));
        //databaseBenchmark();
        initBenchmark();
    }
}

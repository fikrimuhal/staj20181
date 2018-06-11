package com.hivecdn.androidp2p;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static Context context;
    Button senderButton, receiverButton, chatButton;
    Button bsenderButton, breceiverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        senderButton = findViewById(R.id.senderButton);
        receiverButton = findViewById(R.id.receiverButton);
        chatButton = findViewById(R.id.chatButton);
        bsenderButton = findViewById(R.id.benchSender);
        breceiverButton = findViewById(R.id.benchReceiver);
        senderButton.setOnClickListener(this);
        receiverButton.setOnClickListener(this);
        chatButton.setOnClickListener(this);
        bsenderButton.setOnClickListener(this);
        breceiverButton.setOnClickListener(this);
        context = this;
    }

    @Override
    public void onClick(View v) {
        if (v == senderButton) {
            startActivity(new Intent(this, SenderActivity.class));
        }
        else if (v == receiverButton) {
            startActivity(new Intent(this, ReceiverActivity.class));
        }
        else if (v == chatButton) {
            startActivity(new Intent(this, ChatActivity.class));
        }
        else if (v == bsenderButton) {
            startActivity(new Intent(this, BenchSenderActivity.class));
        }
        else if (v == breceiverButton) {
            startActivity(new Intent(this, BenchReceiverActivity.class));
        }
    }
}

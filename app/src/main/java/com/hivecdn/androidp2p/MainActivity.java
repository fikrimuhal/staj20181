package com.hivecdn.androidp2p;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button senderButton, receiverButton, chatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        senderButton = findViewById(R.id.senderButton);
        receiverButton = findViewById(R.id.receiverButton);
        chatButton = findViewById(R.id.chatButton);
        senderButton.setOnClickListener(this);
        receiverButton.setOnClickListener(this);
        chatButton.setOnClickListener(this);
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
    }
}

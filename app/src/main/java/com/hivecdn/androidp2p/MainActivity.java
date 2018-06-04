package com.hivecdn.androidp2p;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.webrtc.Logging;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ChatClient.MyInterface{

    final String TAG = MainActivity.class.getName();

    Button sendButton, connectButton, acceptButton, denyButton;
    EditText messageEdit;
    TextView messageBox;
    ChatClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendButton = findViewById(R.id.sendButton);
        acceptButton = findViewById(R.id.acceptButton);
        denyButton = findViewById(R.id.denyButton);
        connectButton = findViewById(R.id.connectButton);
        messageEdit = findViewById(R.id.messageEdit);
        messageBox = findViewById(R.id.messageBox);
        sendButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);
        denyButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        sendButton.setEnabled(false);
        acceptButton.setEnabled(false);
        denyButton.setEnabled(false);
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements())
                {
                    InetAddress i = (InetAddress) ee.nextElement();
                    Log.d(TAG, i.getHostAddress());
                }
            }
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    void sendButtonClick()
    {
        if (messageEdit.getText().toString().length() == 0)
            return ;
        client.sendMessage(messageEdit.getText().toString());
    }

    void connectButtonClick() {
        if (messageEdit.getText().toString().length() == 0)
            return ;
        client = new ChatClient(getApplicationContext(), messageEdit.getText().toString(), this);
        connectButton.setEnabled(false);
    }

    void acceptButtonClick() {
        client.acceptRequest();
        acceptButton.setEnabled(false);
        denyButton.setEnabled(false);
    }

    void denyButtonClick() {
        client.denyRequest();
        acceptButton.setEnabled(false);
        denyButton.setEnabled(false);
    }

    public void onVerbose(final String msg) {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                messageBox.append("<"+msg+">");
            }
        }));
    }

    public void onMessage(final String msg) {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                messageBox.append("Message:\""+msg+"\"");
            }
        }));
    }

    public void onConnectionRequest(final String otherId) {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                messageBox.append("<Got connection request from peerId: " + otherId + ">");
                acceptButton.setEnabled(true);
                denyButton.setEnabled(true);
            }
        }));
    }
    public void onIdReceived(final String ourId) {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                messageBox.append("<Our peerId is: " + ourId + ">");
            }
        }));
    }

    public void onConnected() {
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                messageBox.append("<Connected>");
                sendButton.setEnabled(true);
            }
        }));
    }

    @Override
    public void onClick(View view) {
        if (view == sendButton)
            sendButtonClick();
        if (view == acceptButton)
            acceptButtonClick();
        if (view == denyButton)
            denyButtonClick();
        if (view == connectButton)
            connectButtonClick();
    }
}

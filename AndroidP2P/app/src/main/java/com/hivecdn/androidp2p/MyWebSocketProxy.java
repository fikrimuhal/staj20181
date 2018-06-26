package com.hivecdn.androidp2p;

import android.support.annotation.Nullable;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by karta on 5/30/2018.
 */

public class MyWebSocketProxy extends WebSocketListener {
    private MyWebSocketListener list;
    public MyWebSocketProxy(MyWebSocketListener _list) {
        super();
        list = _list;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        list.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        list.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        list.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        list.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        list.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        list.onFailure(webSocket, t, response);
    }
}

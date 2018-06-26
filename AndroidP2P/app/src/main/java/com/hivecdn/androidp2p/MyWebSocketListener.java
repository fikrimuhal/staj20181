package com.hivecdn.androidp2p;

import android.support.annotation.Nullable;

import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

/**
 * Created by karta on 5/30/2018.
 */

public interface MyWebSocketListener {
    public void onOpen(WebSocket webSocket, Response response);
    public void onMessage(WebSocket webSocket, String text);
    public void onMessage(WebSocket webSocket, ByteString bytes);
    public void onClosing(WebSocket webSocket, int code, String reason);
    public void onClosed(WebSocket webSocket, int code, String reason);
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response);
}


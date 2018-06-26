package com.hivecdn.androidp2p;

import android.util.Log;

import java.nio.ByteBuffer;

abstract public interface P2PProtocolMessage {

    final String TAG = P2PProtocolMessage.class.getName();

    public ByteBuffer toBinary();

    static public P2PProtocolMessage fromBinary(ByteBuffer buf) {
        final int msgType = buf.get();
        if (msgType == 10) {
            return new RangeRequest(buf.getInt(), buf.getInt());
        }
        else if (msgType == 11) {
            return new RangeResponse(buf.getInt(), buf.getInt());
        }
        else {
            Log.v(TAG, "Unrecognized byte: " + msgType + ", length: " + buf.limit() + ", position: " + buf.position() + "going to throw an exception.");
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<15;i++)
                sb.append(buf.get(i));
            Log.v(TAG, "Message bytes: " + sb.toString());
            throw new IllegalArgumentException();
        }
    }
}

package com.hivecdn.androidp2p;

import java.nio.ByteBuffer;

abstract public interface P2PProtocolMessage {
    public ByteBuffer toBinary();

    static public P2PProtocolMessage fromBinary(ByteBuffer buf) {
        final int msgType = buf.get();
        if (msgType == 10) {
            return new RangeRequest(buf.getInt(), buf.getInt());
        }
        else if (msgType == 11) {
            return new RangeResponse(buf.getInt(), buf.getInt());
        }
        else
            throw new IllegalArgumentException();
    }
}

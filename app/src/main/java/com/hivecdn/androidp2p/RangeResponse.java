package com.hivecdn.androidp2p;

import java.nio.ByteBuffer;

public class RangeResponse implements P2PProtocolMessage {
    public int start, len;

    @Override
    public ByteBuffer toBinary() {
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put((byte)11);
        buf.putInt(start);
        buf.putInt(len);
        return buf;
    }

    public RangeResponse(int _start, int _len) {
        start = _start;
        len = _len;
    }
}

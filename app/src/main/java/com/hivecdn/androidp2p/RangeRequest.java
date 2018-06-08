package com.hivecdn.androidp2p;

import java.nio.ByteBuffer;

public class RangeRequest implements P2PProtocolMessage {
    public int start, len;

    @Override
    public ByteBuffer toBinary() {
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put(0, (byte)10);
        buf.putInt(1, start);
        buf.putInt(5, len);
        buf.position(0);
        return buf;
    }

    public RangeRequest(int _start, int _len) {
        start = _start;
        len = _len;
    }
}

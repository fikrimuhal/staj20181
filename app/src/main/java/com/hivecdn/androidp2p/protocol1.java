package com.hivecdn.androidp2p;

import java.nio.ByteBuffer;

public class protocol1 {
    public interface MyMessage {};
    public class RequestRange implements MyMessage {
        public int start, len;
    }
    public class RangeResponse implements MyMessage{
        public int start, len;
    }

    public ByteBuffer toBinary(MyMessage _msg) {
        if (_msg instanceof RequestRange) {
            RequestRange msg = (RequestRange)_msg;
            ByteBuffer buf = ByteBuffer.allocate(9);
            buf.put(0, (byte)10);
            buf.putInt(1, msg.start);
            buf.putInt(5, msg.len);
            return buf;
        }
        else if (_msg instanceof RangeResponse) {
            RequestRange msg = (RequestRange)_msg;
            ByteBuffer buf = ByteBuffer.allocate(9);
            buf.put(0, (byte)11);
            buf.putInt(1, msg.start);
            buf.putInt(5, msg.len);
            return buf;
        }
        else
            throw new IllegalArgumentException();
    }

    public MyMessage fromBinary(ByteBuffer buf) {
        if (buf.get(0) == 10) {
            RequestRange msg = new RequestRange();
            msg.start = buf.getInt(1);
            msg.len = buf.getInt(5);
            return msg;
        }
        else if (buf.get(0) == 11) {
            RangeResponse msg = new RangeResponse();
            msg.start = buf.getInt(1);
            msg.len = buf.getInt(5);
            return msg;
        }
        else
            throw new IllegalArgumentException();
    }
}

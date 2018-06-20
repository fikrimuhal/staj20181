package com.hivecdn.androidp2p;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

/**
 * Created by karta on 5/31/2018.
 */

public class VideoPeerConnection extends WebRtcPeerConnection {

    final String TAG = VideoPeerConnection.class.getName();
    final int MaxNumVideoBytesInSinglePacket = 1024*15; // 15 kbyte

    public interface VideoPeerConnectionListener {
        void onRequest(VideoPeerConnection vpc, int start, int len);
        void onResponse(VideoPeerConnection vpc, ByteBuffer buf, int start, int len);
    }

    VideoPeerConnectionListener iface;

    public VideoPeerConnection(SignalingServerConnection _ssc, Context _context, String _url, VideoPeerConnectionListener _iface, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        super(_ssc, _context, _url, _ourPeerId, _ourSessionId, _signalId, _otherPeerId, _otherSessionId, _creatingOffer, offerPayload);
        iface = _iface;
    }

    public void sendRange(byte[] bytes, int start, int len) {
        if (dChannel == null)
            return ;
        int numBytesSent = 0;
        while (numBytesSent < len) {
            int thisTimeNumBytesSent = Math.min(MaxNumBytesInSinglePacket, len-numBytesSent);
            ByteBuffer header = new RangeResponse(start+numBytesSent, thisTimeNumBytesSent).toBinary();
            ByteBuffer all = ByteBuffer.allocate(header.limit() + thisTimeNumBytesSent);
            all.put(header);
            all.put(bytes, numBytesSent, thisTimeNumBytesSent);
            all.position(0); // Without this line, Webrtc library only sends the last 9 bytes.
            dChannel.send(new DataChannel.Buffer(all, true));
            numBytesSent += thisTimeNumBytesSent;
        }
    }

    public void requestRange(int start, int len) {
        if (dChannel == null) {
            Log.v(TAG, "Can't send message, dChannel == null");
            return;
        }
        ByteBuffer header = new RangeRequest(start, len).toBinary();
        dChannel.send(new DataChannel.Buffer(header, true));
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        super.onMessage(buffer);

        if (false == buffer.binary) {
            Log.v(TAG, "Invalid message recevied, must be binary.");
            return ;
        }
        P2PProtocolMessage msg = null;
        try {
            msg = P2PProtocolMessage.fromBinary(buffer.data);
        }
        catch (IllegalArgumentException e) {
            Log.v(TAG, "Invalid message recevied.");
            return ;
        }
        if (msg instanceof RangeRequest) {
            Log.v(TAG, "Got new request");
            iface.onRequest(this, ((RangeRequest) msg).start, ((RangeRequest) msg).len);
        }
        else if (msg instanceof RangeResponse) {
            if (((RangeResponse) msg).len != buffer.data.remaining()) {
                Log.w(TAG, "Message's claimed length doesn't match its actual length. Ignoring.");
                return ;
            }
            ByteBuffer data = ByteBuffer.allocate(buffer.data.remaining());
            data.put(buffer.data);
            data.position(0);
            iface.onResponse(this, data, ((RangeResponse) msg).start, ((RangeResponse) msg).len);
        }
    }
}

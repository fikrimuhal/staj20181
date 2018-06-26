package com.hivecdn.androidp2p;

import android.content.Context;

import org.json.JSONObject;
import org.webrtc.DataChannel;

public class BinaryWebRtcPeerConnection extends WebRtcPeerConnection {

    interface BinaryWebRtcListenerInterface {
        public void onMessage(WebRtcPeerConnection pc, DataChannel.Buffer buffer);
    }

    BinaryWebRtcListenerInterface iface;

    BinaryWebRtcPeerConnection(BinaryWebRtcListenerInterface _iface, SignalingServerConnection _ssc, Context _context, String _url, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        super(_ssc, _context, _url, _ourPeerId, _ourSessionId, _signalId, _otherPeerId, _otherSessionId, _creatingOffer, offerPayload);
        iface = _iface;
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        super.onMessage(buffer);
        iface.onMessage(this, buffer);
    }
}


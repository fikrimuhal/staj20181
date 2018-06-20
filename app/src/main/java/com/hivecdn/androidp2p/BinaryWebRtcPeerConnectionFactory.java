package com.hivecdn.androidp2p;

import android.content.Context;

import org.json.JSONObject;

class BinaryWebRtcPeerConnectionFactory implements WebRtcPeerConnectionFactoryInterface {

    BinaryWebRtcPeerConnection.BinaryWebRtcListenerInterface iface;

    BinaryWebRtcPeerConnectionFactory(BinaryWebRtcPeerConnection.BinaryWebRtcListenerInterface _iface) {
        iface = _iface;
    }

    @Override
    public WebRtcPeerConnection Create(SignalingServerConnection _ssc, Context _context, String _url, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        return new BinaryWebRtcPeerConnection(iface, _ssc, _context, _url, _ourPeerId, _ourSessionId, _signalId, _otherPeerId, _otherSessionId, _creatingOffer, offerPayload);
    }
}



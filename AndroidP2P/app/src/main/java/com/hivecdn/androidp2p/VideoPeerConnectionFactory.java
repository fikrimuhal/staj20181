package com.hivecdn.androidp2p;

import android.content.Context;

import org.json.JSONObject;

public class VideoPeerConnectionFactory implements WebRtcPeerConnectionFactoryInterface {

    VideoPeerConnection.VideoPeerConnectionListener listener;

    VideoPeerConnectionFactory(VideoPeerConnection.VideoPeerConnectionListener _listener) {
        listener = _listener;
    }

    @Override
    public WebRtcPeerConnection Create(SignalingServerConnection _ssc, Context _context, String _url, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        return new VideoPeerConnection(_ssc, _context, _url, listener, _ourPeerId, _ourSessionId, _signalId, _otherPeerId, _otherSessionId, _creatingOffer, offerPayload);
    }
}

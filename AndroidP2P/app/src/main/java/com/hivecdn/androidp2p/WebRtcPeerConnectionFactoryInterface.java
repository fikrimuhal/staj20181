package com.hivecdn.androidp2p;

import android.content.Context;

import org.json.JSONObject;

public interface WebRtcPeerConnectionFactoryInterface {
    public WebRtcPeerConnection Create(SignalingServerConnection _ssc, Context _context, String _url, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload);
}

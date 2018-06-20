package com.hivecdn.androidp2p;

import android.content.Context;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by karta on 5/31/2018.
 */

public class WebRtcPeerConnection implements  PeerConnection.Observer, SdpObserver, DataChannel.Observer{

    final String TAG = WebRtcPeerConnection.class.getName();
    final int MaxNumBytesInSinglePacket = 1024*16; // 16 kbyte

    boolean creatingOffer;
    String otherPeerId;
    int otherSessionId;
    String peerId;
    String signalId; // signalId associated with the ongoing handshake.
    int sessionId;
    PeerConnection peerConnection;
    String url;
    Context context;
    DataChannel dChannel;
    SignalingServerConnection ssc;

    public WebRtcPeerConnection(SignalingServerConnection _ssc, Context _context, String _url, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        ssc = _ssc;
        context = _context;
        url = _url;
        peerId = _ourPeerId;
        sessionId = _ourSessionId;
        signalId = _signalId;
        otherPeerId = _otherPeerId;
        otherSessionId = _otherSessionId;
        creatingOffer = _creatingOffer;
        if (creatingOffer) {
            peerConnection = createPeerConnection();
            DataChannel.Init init = new DataChannel.Init();
            Log.v(TAG, "Creating data channel");
            dChannel = peerConnection.createDataChannel("VideoPeerConnection", init);
            if (dChannel == null) {
                Log.v(TAG, "Failed to create data channel.");
                return;
            } else
                dChannel.registerObserver(this);
            Log.v(TAG, "Creating offer description");
            peerConnection.createOffer(this, new MediaConstraints());
        }
        else {
            peerConnection = createPeerConnection();
            SessionDescription remoteDesc;
            try {
                remoteDesc = new SessionDescription(SessionDescription.Type.OFFER, offerPayload.getJSONObject("payload").getString("sdp"));
            }
            catch (JSONException e) {
                Log.v(TAG, "JSONException");
                return ;
            }
            Log.v(TAG, "Setting remote description");
            peerConnection.setRemoteDescription(this, remoteDesc);
            Log.v(TAG, "Creating answer description");
            peerConnection.createAnswer(this, new MediaConstraints());
        }
    }

    public void onIncomingAnswer(JSONObject payload) {
        if (peerConnection == null)
            return ; // No handshake is going on.
        SessionDescription remoteDesc;
        try {
            remoteDesc = new SessionDescription(SessionDescription.Type.ANSWER, payload.getJSONObject("payload").getString("sdp"));
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return ;
        }
        Log.v(TAG, "Setting remote description");
        peerConnection.setRemoteDescription(this, remoteDesc);
    }

    public void onIncomingCandidate(JSONObject payload) {
        Log.v(TAG, "Incoming candidate.");
        IceCandidate candidate;
        String sdp;
        try {
            if (payload.getString("otherPeerId").equals(otherPeerId) == false) {
                Log.v(TAG, "Wrong peerId");
                return ;
            }
            JSONObject innerPayload = payload.getJSONObject("payload");
            sdp = innerPayload.getString("candidate");
            candidate = new IceCandidate(innerPayload.getString("sdpMid"), innerPayload.getInt("sdpMLineIndex"), sdp);
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return ;
        }
        Log.v(TAG, "Adding ice candidate");
        peerConnection.addIceCandidate(candidate);
    }

    public void sendMessage(DataChannel.Buffer buffer) {
        if (dChannel == null)
            return ;
        if (buffer.data.remaining() > MaxNumBytesInSinglePacket)
            throw new IllegalArgumentException();
        dChannel.send(buffer);
    }

    public PeerConnection createPeerConnection() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions());
        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
        List<PeerConnection.IceServer> serverList = new ArrayList<PeerConnection.IceServer>();
        serverList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        return factory.createPeerConnection(serverList, this);
    }

    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.v(TAG, "Created sdp, setting local descr.");
        peerConnection.setLocalDescription(this, origSdp);
        ssc.onGotSDP(this, origSdp);
    }

    @Override
    public void onSetSuccess() {
        Log.v(TAG, "onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.v(TAG, "onCreateFailure");
    }

    @Override
    public void onSetFailure(String s) {
        Log.v(TAG, "onSetFailure");
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.v(TAG, "onSignalingChange: " + signalingState.name());
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.v(TAG, "onIceConnectionChange: " + iceConnectionState.name());
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
            //ssc.onPeerConnected(this); // We need to wait for the data channel.
        }
        else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            Log.v(TAG, "Disconnected");
            otherPeerId = null;
            signalId = null;
            peerConnection = null;
            dChannel = null;
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.v(TAG, "onIceConnectionReceivingChange");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.v(TAG, "onIceGatheringChange: " + iceGatheringState.name());
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.v(TAG, "onIceCandidate");
        ssc.onGotIceCandidate(this, iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] ıceCandidates) {
        Log.v(TAG, "onIceCandidateRemoved");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.v(TAG, "onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.v(TAG, "onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.v(TAG, "onDataChannel: " + dataChannel.hashCode());
        dataChannel.registerObserver(this);
        dChannel = dataChannel;
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.v(TAG, "onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.v(TAG, "onAddTrack");
    }

    @Override
    public void onBufferedAmountChange(long l) {
        Log.v(TAG, "onBufferedAmountChange: " + l);
    }

    @Override
    public void onStateChange() {
        if (dChannel == null) {
            Log.v(TAG, "onStateChange: dChannel == null");
            return ;
        }
        Log.v(TAG, "onStateChange: " + dChannel.state().name());
        if (dChannel.state() == DataChannel.State.OPEN)
            ssc.onPeerConnected(this);
        else if (dChannel.state() == DataChannel.State.CLOSED)
            ssc.onPeerDisconnected(this);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        // Meant to be overridden
    }

    public void close() {
        if (peerConnection != null)
            peerConnection.close();
    }

}

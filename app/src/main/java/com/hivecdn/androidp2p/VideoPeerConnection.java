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

public class VideoPeerConnection implements  PeerConnection.Observer, SdpObserver, DataChannel.Observer{

    final String TAG = VideoPeerConnection.class.getName();
    final int MaxNumBytesInSinglePacket = 1024*16; // 16 kbyte

    public interface MyInterface {
        //void onVerbose(String msg);
        void onRequest(VideoPeerConnection vpc, int start, int len);
        void onResponse(VideoPeerConnection vpc, ByteBuffer buf, int start, int len);
        //void onConnected(String otherId); // SignalingListener's will get this message via SignalingListener.
    }

    boolean creatingOffer;
    String otherPeerId;
    int otherSessionId;
    String peerId;
    String signalId; // signalId associated with the ongoing handshake.
    int sessionId;
    PeerConnection peerConnection;
    String url;
    Context context;
    MyInterface iface;
    DataChannel dChannel;
    SignalingServerConnection ssc;
    boolean receivedAnswer;

    public VideoPeerConnection(SignalingServerConnection _ssc, Context _context, String _url, MyInterface _iface, String _ourPeerId, int _ourSessionId, String _signalId, String _otherPeerId, int _otherSessionId, boolean _creatingOffer, JSONObject offerPayload) {
        ssc = _ssc;
        context = _context;
        url = _url;
        iface = _iface;
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
            /*if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                Log.v(TAG, "Wrong signalId on the answer");
                return; // Wrong signal id. Ignore.
            }*/ // SignalingServerConnection will make sure that each VideoPeerConnection will only receive handshake events pertinent to it.
            remoteDesc = new SessionDescription(SessionDescription.Type.ANSWER, payload.getJSONObject("payload").getString("sdp"));
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return ;
        }
        Log.v(TAG, "Setting remote description");
        receivedAnswer = true;
        peerConnection.setRemoteDescription(this, remoteDesc);
    }

    /*void onIncomingOffer(JSONObject payload) {
        Log.v(TAG, "Incoming offer");
        if (peerId == null)
            return; // We don't know our peerId yet.
        if (peerConnection != null) // TODO: We need to seperate this class into two classes: One manages connection with the signaling server, the other manages Webrtc PeerConnection's
            return ; // There already is a handshake going on.
        creatingOffer = false;
        try {
            otherSessionId = payload.getInt("otherSessionId");
            otherPeerId = payload.getString("otherPeerId");
            signalId = payload.getJSONObject("payload").getString("signalId");
            peerConnection = createPeerConnection();
            SessionDescription remoteDesc = new SessionDescription(SessionDescription.Type.OFFER, payload.getJSONObject("payload").getString("sdp"));
            Log.v(TAG, "Setting remote description");
            peerConnection.setRemoteDescription(this, remoteDesc);
            Log.v(TAG, "Creating answer description");
            peerConnection.createAnswer(this, new MediaConstraints());
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
        }
    }*/

    public void onIncomingCandidate(JSONObject payload) {
        Log.v(TAG, "Incoming candidate.");
        addCandidateFromPayload(payload);
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
            /*StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 15; i++)
                sb.append(all.get(i));
            Log.v(TAG, "sendRange: Message to be sent: " + sb.toString());*/
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

    void addCandidateFromPayload(JSONObject payload) {
        IceCandidate candidate;
        String sdp;
        try {
            /*if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                Log.v(TAG, "Wrong signalId");
                return ;
            }*/
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
        //iface.onConnected(otherPeerId);
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
        if (buffer.binary == false) {
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

    public void close() {
        if (peerConnection != null)
            peerConnection.close();
    }

}

package com.hivecdn.androidp2p;

import android.content.Context;
import android.os.SystemClock;
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
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.net.URLEncoder;

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

public class ChatClient implements  MyWebSocketListener, PeerConnection.Observer, SdpObserver, DataChannel.Observer{

    final String TAG = ChatClient.class.getName();

    public interface MyInterface {
        void onVerbose(String msg);
        void onMessage(String msg);
        void onConnectionRequest(String otherId);
        void onIdReceived(String ourId);
        void onConnected();
    }

    boolean creatingOffer;
    String otherPeerId;
    int otherSessionId;
    String sessionToken;
    String peerId;
    String signalId; // signalId associated with the ongoing handshake.
    int sessionId;
    WebSocket socket;
    MyWebSocketProxy proxy;
    OkHttpClient client;
    PeerConnection peerConnection;
    String name;
    JSONObject offerOnHold;
    Context context;
    boolean userDeciding;
    MyInterface iface;
    DataChannel dChannel;
    ArrayList<JSONObject> candidatesOnHold;
    ArrayList<JSONObject> ourCandidatesOnHold;
    boolean receivedAnswer;

    public ChatClient(Context _context, String _name, MyInterface _iface) {
        context = _context;
        name = _name;
        iface = _iface;
        proxy = new MyWebSocketProxy(this);
        candidatesOnHold = new ArrayList<JSONObject>();
        ourCandidatesOnHold = new ArrayList<JSONObject>();
        client = new OkHttpClient.Builder()
                .readTimeout(0,  TimeUnit.MILLISECONDS)
                .build();
        getWebsocketAddress();
    }

    void getWebsocketAddress()
    {
        Log.v(TAG, "Getting websocket addresss");
        Request request = new Request.Builder()
                .url("https://static.hivecdn.com/host")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v(TAG, "Failed to get websocket address.");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.v(TAG, "Unexpected code while trying to get websocket address:" + response);
                } else {
                    String address;
                    try {
                        JSONObject jObject = new JSONObject(response.body().string());
                        address = jObject.getJSONArray("hosts").getJSONObject(0).getString("address");
                    } catch (JSONException e) {
                        Log.v(TAG, "JSONException");
                        return;
                    }
                    connectWebSocket(address);
                }
            }
        });
    }

    void connectWebSocket(String s)
    {
        final String addr ="wss://"+s+"/ws";
        Log.v(TAG, "Connecting to websocket: " + addr);
        Request request = new Request.Builder()
                .url(addr)
                .addHeader("Origin", "https://hivecdn.com")
                .build();
        client.newWebSocket(request, proxy);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        socket = webSocket;
        Log.v(TAG, "Websocket connected!");
        sendAuth();
        setPingTimeout();
    }

    void setPingTimeout()
    {
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                if (socket != null)
                {
                    socket.send("{\"payload\":{},\"command\":\"bogazici.ping\"}");
                    Log.v(TAG, "Sending ping message");
                }
            }
        },5000,15000);
    }

    void sendAuth() {
        final String encodedName = URLEncoder.encode(name);
        final String url = "https://stajurl.com/androidChatApp/" + encodedName;
        Log.v(TAG, "Sending authentication etc.");
        socket.send("{\"payload\":{\"siteId\":\"hivecdn-0000-0000-0000\",\"deviceType\":\"androidApp\",\"caps\":{\"webRTCSupport\":true,\"wsSupport\":true}},\"command\":\"bogazici.Authentication\"}");
        JSONObject msg = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            payload.put("url", url);
            msg.put("command", "VideoURLChanged");
            msg.put("payload", payload);
            socket.send(msg.toString());
            msg = new JSONObject();
            payload = new JSONObject();
            payload.put("levelCount", 9);
            payload.put("segmentCount", 0);
            payload.put("url", url);
            payload.put("streamType", "VOD");
            msg.put("payload", payload);
            msg.put("command", "peer.VideoDashMeta");
            socket.send(msg.toString());
            msg = new JSONObject();
            payload = new JSONObject();
            payload.put("videoId", url);
            payload.put("playing", true);
            payload.put("playbackSpeed", 1);
            payload.put("playerPosition", 0);
            payload.put("persist", true);
            msg.put("payload", payload);
            msg.put("command", "PeerPlayerState");
        }
        catch (JSONException e) {
            Log.v(TAG, "Unexpected JSONException");
            return ;
        }
        socket.send(msg.toString());
        socket.send("{\"payload\":{\"state\":\"playing\",\"currentTime\":0,\"timestamp\":1527689052511,\"isMuted\":false,\"playbackSpeed\":1},\"command\":\"PlayerState\"}");
    }

    void authenticationResponse(JSONObject jObject) {
        try {
            jObject = jObject.getJSONObject("payload");
            peerId = jObject.getString("peerId");
            sessionId = jObject.getInt("sessionId");
            iface.onIdReceived(peerId);
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.v(TAG, "JSONException");
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.v(TAG, "Inmsg: " + text);

        String command;
        JSONObject jObject;
        try {
            jObject = new JSONObject(text);
            command = jObject.getString("command");
        } catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return;
        }
        if (command.equals("bogazici.sessionToken"))
            sessionTokenReceived(jObject);
        if (command.equals("AuthenticationResponse"))
            authenticationResponse(jObject);
        if (command.equals("peer.MakeConnectionWithPeer"))
            makeConnectionWithPeer(jObject);
        if (command.equals("peer.WebRTCHandshake"))
            incomingHandshake(jObject);
    }

    void incomingOffer(JSONObject payload) {
        Log.v(TAG, "Incoming offer");
        if (peerId == null)
            return; // We don't know our peerId yet.
        if (peerConnection != null)
            return ; // There already is a handshake going on.
        if (userDeciding) // There already has been a connection request, and the user hasn't decided yet.
            return ;
        offerOnHold = payload; // Store the payload so that if the user decides to accept the connection, we can remember relevant information.
        creatingOffer = false;
        try {
            otherSessionId = payload.getInt("otherSessionId");
            otherPeerId = payload.getString("otherPeerId");
            signalId = payload.getJSONObject("payload").getString("signalId");
            userDeciding = true;
            iface.onConnectionRequest(payload.getString("otherPeerId"));
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
        }
    }
    public void sendMessage(String msg) {
        if (dChannel != null)
            dChannel.send(new DataChannel.Buffer(ByteBuffer.wrap(msg.getBytes()), false));
    }
    public void denyRequest() {
        userDeciding = false;
    }

    void addCandidateFromPayload(JSONObject payload) {
        IceCandidate candidate;
        String sdp;
        try {
            if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                Log.v(TAG, "Wrong signalId");
                return ;
            }
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

    public void acceptRequest() {
        if (userDeciding == false)
            return ;
        userDeciding = false;
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions());
        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
        List<PeerConnection.IceServer> serverList = new ArrayList<PeerConnection.IceServer>();
        serverList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        peerConnection = factory.createPeerConnection(serverList, this);
        if (creatingOffer) {
            DataChannel.Init init = new DataChannel.Init();
            Log.v(TAG, "Creating data channel");
            dChannel = peerConnection.createDataChannel("test", init);
            if (dChannel == null) {
                Log.v(TAG, "Failed to create data channel.");
                return;
            } else
                dChannel.registerObserver(this);
            Log.v(TAG, "Creating offer description");
            peerConnection.createOffer(this, new MediaConstraints());
        }
        else {
            try {
                SessionDescription remoteDesc = new SessionDescription(SessionDescription.Type.OFFER, offerOnHold.getJSONObject("payload").getString("sdp"));
                Log.v(TAG, "Setting remote description");
                peerConnection.setRemoteDescription(this, remoteDesc);
            } catch (JSONException e) {
                Log.v(TAG, "JSONException");
                return;
            }
            Log.v(TAG, "Creating answer description");
            peerConnection.createAnswer(this, new MediaConstraints());
        }
    }

    void incomingAnswer(JSONObject payload) {
        Log.v(TAG, "Incoming answer.");
        if (peerConnection == null)
            return ; // No handshake is going on.
        SessionDescription remoteDesc;
        try {
            if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                Log.v(TAG, "Wrong signalId on the answer");
                return; // Wrong signal id. Ignore.
            }
            remoteDesc = new SessionDescription(SessionDescription.Type.ANSWER, payload.getJSONObject("payload").getString("sdp"));
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return ;
        }
        Log.v(TAG, "Setting remote description");
        receivedAnswer = true;
        peerConnection.setRemoteDescription(this, remoteDesc);
        for (JSONObject msg : ourCandidatesOnHold) {
            Log.v(TAG, "Sending on hold candidate.");
            socket.send(msg.toString());
        }
        ourCandidatesOnHold.clear();
    }

    void incomingCandidate(JSONObject payload) {
        Log.v(TAG, "Incoming candidate.");
        if (peerConnection == null) {
            Log.v(TAG, "Putting on hold. No connection yet.");
            candidatesOnHold.add(payload);
            //if (candidatesOnHold.size() > 25)
            //candidatesOnHold.subList(0, 15).clear();
            return ;
        }
        addCandidateFromPayload(payload);
    }

    void incomingHandshake(JSONObject jObject) {
        try {
            jObject = jObject.getJSONObject("payload");
            if (jObject.getString("type").equals("OFFER")) {
                incomingOffer(jObject);
                return;
            }
            if (jObject.getString("type").equals("ANSWER")) {
                incomingAnswer(jObject);
                return ;
            }
            if (jObject.getString("type").equals("CANDIDATE")) {
                incomingCandidate(jObject);
                return ;
            }
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
        }
    }

    void makeConnectionWithPeer(JSONObject res) {
        Log.v(TAG, "makeConnectionWithPeer");
        if (peerId == null)
            return; // We don't know our peerId yet.
        if (peerConnection != null) // There already is a connection.
            return;
        if (userDeciding)
            return ;
        try {
            res = res.getJSONObject("payload");
            String uploaderPeerId = res.getString("uploaderPeerId");
            String downloaderPeerId = res.getString("downloaderPeerId");
            int uploaderSessionId = res.getInt("uploaderSessionId");
            int downloaderSessionId = res.getInt("downloaderSessionId");
            if (peerId.equals(uploaderPeerId)) {
                otherPeerId = downloaderPeerId;
                otherSessionId = downloaderSessionId;
            } else if (peerId.equals(downloaderPeerId)) {
                otherPeerId = uploaderPeerId;
                otherSessionId = uploaderSessionId;
            }
            creatingOffer = true;
            userDeciding = true;
            iface.onConnectionRequest(otherPeerId);
        } catch (JSONException e) {
            Log.v(TAG, "JSONException");
            return;
        }
    }

    void sessionTokenReceived(JSONObject res) {
        try {
            sessionToken = res.getJSONObject("payload").getString("sessionToken");
        }
        catch (JSONException e) {
            Log.v(TAG, "JSONException");
        }
    }


    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.v(TAG, "Binary message received");
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {

    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.v(TAG, "Closed");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        Log.v(TAG, "Error");
        t.printStackTrace();
    }

    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.v(TAG, "Created sdp, setting local descr.");
        peerConnection.setLocalDescription(this, origSdp);
        JSONObject message;
        try {
            JSONObject innerPayload = new JSONObject();
            innerPayload.put("remoteVersion", "2.2.7-SNAPSHOT");
            innerPayload.put("sdp", origSdp.description);
            if (creatingOffer) {
                signalId = UUID.randomUUID().toString();
                innerPayload.put("type", "offer");
            }
            else
                innerPayload.put("type", "answer");
            innerPayload.put("signalId", signalId);
            JSONObject payload = new JSONObject();
            payload.put("otherPeerId", otherPeerId);
            payload.put("otherSessionId", otherSessionId);
            if (creatingOffer)
                payload.put("type", "OFFER");
            else
                payload.put("type", "ANSWER");
            payload.put("payload", innerPayload);
            message = new JSONObject();
            message.put("command", "peer.WebRTCHandshake");
            message.put("payload", payload);
        }
        catch (JSONException e) {
            Log.v(TAG, "Unexpected JSONException");
            return ;
        }
        if (creatingOffer)
            Log.v(TAG, "Sending offer.");
        else
            Log.v(TAG, "Sending answer.");
        Log.v(TAG, "Sending: " + message.toString());
        socket.send(message.toString());
    }

    @Override
    public void onSetSuccess() {
        Log.v(TAG, "onSetSuccess");
        for (JSONObject payload : candidatesOnHold) {
            addCandidateFromPayload(payload);
        }
        candidatesOnHold.clear();
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
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED)
            iface.onConnected();
        else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            iface.onVerbose("Disconnected");
            otherPeerId = null;
            signalId = null;
            peerConnection = null;
            offerOnHold = null;
            userDeciding = false;
            dChannel = null;
            candidatesOnHold.clear();
            ourCandidatesOnHold.clear();
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
        JSONObject message;
        try {
            JSONObject innerPayload = new JSONObject();
            innerPayload.put("remoteVersion", "2.2.7-SNAPSHOT");
            innerPayload.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            innerPayload.put("sdpMid", iceCandidate.sdpMid);
            innerPayload.put("candidate", iceCandidate.sdp);
            innerPayload.put("signalId", signalId);
            JSONObject payload = new JSONObject();
            payload.put("otherPeerId", otherPeerId);
            payload.put("otherSessionId", otherSessionId);
            payload.put("type", "CANDIDATE");
            payload.put("payload", innerPayload);
            message = new JSONObject();
            message.put("command", "peer.WebRTCHandshake");
            message.put("payload", payload);
        }
        catch (JSONException e) {
            Log.v(TAG, "Unexpected JSONException");
            return ;
        }
        if (creatingOffer && receivedAnswer==false) {
            Log.v(TAG, "Putting candidate on hold.");
            ourCandidatesOnHold.add(message);
        }
        else {
            Log.v(TAG, "Sending ice candidate");
            Log.v(TAG, "Sending: " + message.toString());
            socket.send(message.toString());
        }
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
        //iface.onConnected();
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
        Log.v(TAG, "onBufferedAmountChange");
    }

    @Override
    public void onStateChange() {
        Log.v(TAG, "onStateChange");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        StringBuilder s = new StringBuilder();
        for (int i=0; i<buffer.data.limit(); i++) {
            s.append((char)buffer.data.get(i));
        }
        iface.onMessage(s.toString());
    }

}

package com.hivecdn.androidp2p;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MyWebSocketListener, PeerConnection.Observer, SdpObserver, DataChannel.Observer{

    final String TAG = MainActivity.class.getName();

    boolean creatingOffer;
    String otherPeerId;
    int otherSessionId;
    boolean role; // true -> uploader, false -> downloader
    String sessionToken;
    String peerId;
    String signalId; // signalId associated with the ongoing handshake.
    int sessionId;
    WebSocket socket;
    MyWebSocketProxy proxy;
    Button sendButton;
    TextView textView;
    EditText editText;
    OkHttpClient client;
    PeerConnection peerConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendButton = findViewById(R.id.sendButton);
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);
        sendButton.setOnClickListener(this);
        proxy = new MyWebSocketProxy(this);
        client = new OkHttpClient.Builder()
            .readTimeout(0,  TimeUnit.MILLISECONDS)
            .build();
        getWebsocketAddress();
    }

    void getWebsocketAddress()
    {
        updateTextView("Getting websocket addresss");
        Request request = new Request.Builder()
                .url("https://static.hivecdn.com/host")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    updateTextView("Unexpected code " + response);
                } else {
                    String address;
                    try {
                        JSONObject jObject = new JSONObject(response.body().string());
                        address = jObject.getJSONArray("hosts").getJSONObject(0).getString("address");
                    } catch (JSONException e) {
                        updateTextView("JSON error.");
                        e.printStackTrace();
                        Log.d("AndroidP2P", response.body().string());
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
        updateTextView("Connecting to websocket: " + addr);
        Request request = new Request.Builder()
            .url(addr)
                .addHeader("Origin", "https://hivecdn.com")
            .build();
        client.newWebSocket(request, proxy);
    }

    void sendButtonClick()
    {
        if (socket != null)
        {
            socket.send(editText.getText().toString());
        }
    }

    @Override
    public void onClick(View view) {
        if (view == sendButton)
            sendButtonClick();
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        socket = webSocket;
        updateTextView("Websocket connected!");
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
        updateTextView("Sending authentication");
        socket.send("{\"payload\":{\"siteId\":\"hivecdn-0000-0000-0000\",\"deviceType\":\"androidApp\",\"caps\":{\"webRTCSupport\":true,\"wsSupport\":true}},\"command\":\"bogazici.Authentication\"}");
        updateTextView("Sending video url");
        socket.send("{\"payload\":{\"url\":\"https://video-dev-cdn.hivecdn.com/videos/dash/TearsOfSteel/2sec/TearsOfSteel_2s_simple_2014_05_09.mpd\"},\"command\":\"VideoURLChanged\"}");
        updateTextView("Sending videodashmeta");
        socket.send("{\"payload\":{\"levelCount\":9,\"segmentCount\":0,\"url\":\"https://video-dev-cdn.hivecdn.com/videos/dash/TearsOfSteel/2sec/TearsOfSteel_2s_simple_2014_05_09.mpd\",\"streamType\":\"VOD\"},\"command\":\"peer.VideoDashMeta\"}");
        updateTextView("Sending peerplayerstate");
        socket.send("{\"payload\":{\"videoId\":\"https://video-dev-cdn.hivecdn.com/videos/dash/TearsOfSteel/2sec/TearsOfSteel_2s_simple_2014_05_09.mpd\",\"playing\":true,\"playbackSpeed\":1,\"playerPosition\":0,\"persist\":true},\"command\":\"PeerPlayerState\"}");
        updateTextView("Sending player state");
        socket.send("{\"payload\":{\"state\":\"playing\",\"currentTime\":0,\"timestamp\":1527689052511,\"isMuted\":false,\"playbackSpeed\":1},\"command\":\"PlayerState\"}");
    }

    void updateTextView(final String s) {
        Log.v(TAG, s);
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                textView.setText(s);
            }
        }));
    }

    void authenticationResponse(JSONObject jObject) {
        try {
            jObject = jObject.getJSONObject("payload");
            peerId = jObject.getString("peerId");
            sessionId = jObject.getInt("sessionId");
        }
        catch(JSONException e) {
            e.printStackTrace();
            updateTextView("JSON exception.");
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
            updateTextView("JSON error.");
            e.printStackTrace();
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
        updateTextView("Incoming offer");
        if (peerId == null)
            return ; // We don't know our peerId yet.
        try {
            otherSessionId = payload.getInt("otherSessionId");
            otherPeerId = payload.getString("otherPeerId");
            signalId = payload.getJSONObject("payload").getString("signalId");
        }
        catch (JSONException e) {
            updateTextView("JSONException");
        }
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions());
        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
        List<PeerConnection.IceServer> serverList = new ArrayList<PeerConnection.IceServer>();
        serverList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        peerConnection = factory.createPeerConnection(serverList, this);
        try {
            SessionDescription remoteDesc = new SessionDescription(SessionDescription.Type.OFFER, payload.getJSONObject("payload").getString("sdp"));
            peerConnection.setRemoteDescription(this, remoteDesc);
        }
        catch (JSONException e) {
            updateTextView("JSONException");
        }
        updateTextView("Creating answer description");
        creatingOffer = false;
        peerConnection.createAnswer(this, new MediaConstraints());
    }

    void incomingAnswer(JSONObject payload) {
        updateTextView("Incoming answer.");
        if (peerConnection == null)
            return ; // No handshake is going on.
        SessionDescription remoteDesc;
        try {
            if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                updateTextView("Wrong signalId on the answer");
                return; // Wrong signal id. Ignore.
            }
            remoteDesc = new SessionDescription(SessionDescription.Type.ANSWER, payload.getJSONObject("payload").getString("sdp"));
        }
        catch (JSONException e) {
            updateTextView("JSONException");
            return ;
        }
        updateTextView("Setting remote description");
        peerConnection.setRemoteDescription(this, remoteDesc);
    }

    void incomingCandidate(JSONObject payload) {
        updateTextView("Incoming candidate.");
        if (peerConnection == null)
            return ; // No handshake is going on.
        IceCandidate candidate;
        try {
            if (payload.getJSONObject("payload").getString("signalId").equals(signalId) == false) {
                updateTextView("Wrong signalId on the answer");
                return; // Wrong signal id. Ignore.
            }
            JSONObject innerPayload = payload.getJSONObject("payload");
            candidate = new IceCandidate(innerPayload.getString("sdpMid"), innerPayload.getInt("sdpMLineIndex"), innerPayload.getString("candidate"));
        }
        catch (JSONException e) {
            updateTextView("JSONException");
            return ;
        }
        updateTextView("Adding ice candidate");
        peerConnection.addIceCandidate(candidate);
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
            updateTextView("JSONException");
        }
    }

    void makeConnectionWithPeer(JSONObject res) {
        updateTextView("makeConnectionWithPeer");
        if (peerId == null) {
            updateTextView("Ignored premature makeConnectionWithPeer request. Buggy backend?");
            return; // We don't know our peerId yet.
        }
        try {
            res = res.getJSONObject("payload");
            String uploaderPeerId = res.getString("uploaderPeerId");
            String downloaderPeerId = res.getString("downloaderPeerId");
            int uploaderSessionId = res.getInt("uploaderSessionId");
            int downloaderSessionId = res.getInt("downloaderSessionId");
            if (uploaderPeerId.startsWith("0248") == false && downloaderPeerId.startsWith("0248")==false) {
                updateTextView("Ignoring this ghost reuqest."); // TODO: What are we getting these ghost requests?
                return;
            }
            if (peerId.equals(uploaderPeerId)) {
                otherPeerId = downloaderPeerId;
                otherSessionId = downloaderSessionId;
                role = true; // We're the uploader
            }
            else if (peerId.equals(downloaderPeerId)) {
                otherPeerId = uploaderPeerId;
                otherSessionId = uploaderSessionId;
                role = false; // We're the downloader
            }
        }
        catch (JSONException e) {
            updateTextView("JSONException");
        }
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions());
        PeerConnectionFactory factory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
        List<PeerConnection.IceServer> serverList = new ArrayList<PeerConnection.IceServer>();
        serverList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        peerConnection = factory.createPeerConnection(serverList, this);
        creatingOffer = true;
        DataChannel.Init init = new DataChannel.Init();
        //init.ordered = true;
        //init.maxRetransmits = 3;
        //init.maxRetransmitTimeMs = 5000;
        //init.protocol = "myprotocol";
        //init.negotiated = false;
        //init.id = 0;
        updateTextView("Creating data channel");
        DataChannel dChannel = peerConnection.createDataChannel("test", init);
        if (dChannel == null) {
            updateTextView("Failed to create data channel.");
            return ;
        }
        else
            dChannel.registerObserver(this);
        updateTextView("Data channel: " + dChannel.hashCode());
        updateTextView("Creating offer description");
        peerConnection.createOffer(this, new MediaConstraints());
    }

    void sessionTokenReceived(JSONObject res) {
        try {
            sessionToken = res.getJSONObject("payload").getString("sessionToken");
        }
        catch (JSONException e) {
            updateTextView("JSONException");
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
        updateTextView("Closed");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        updateTextView("Error");
        t.printStackTrace();
    }

    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        //Log.v(TAG, origSdp.description);
        Log.v(TAG, "onCreateSuccess");
        peerConnection.setLocalDescription(this, origSdp);
        JSONObject message;
        try {
            JSONObject innerPayload = new JSONObject();
            innerPayload.put("remoteVersion", "2.2.6-SNAPSHOT");
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
            updateTextView("Unexpected JSONException");
            return ;
        }
        if (creatingOffer)
            updateTextView("Sending offer.");
        else
            updateTextView("Sending answer.");
        Log.v(TAG, "Sending: " + message.toString());
        socket.send(message.toString());
    }

    @Override
    public void onSetSuccess() {
        updateTextView("onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        updateTextView("onCreateFailure");
    }

    @Override
    public void onSetFailure(String s) {
        updateTextView("onSetFailure");
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        updateTextView("onSignalingChange");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        updateTextView("onIceConnectionChange");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        updateTextView("onIceConnectionReceivingChange");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState ıceGatheringState) {
        updateTextView("onIceGatheringChange");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        updateTextView("onIceCandidate");
        JSONObject message;
        try {
            JSONObject innerPayload = new JSONObject();
            innerPayload.put("remoteVersion", "2.2.6-SNAPSHOT");
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
            updateTextView("Unexpected JSONException");
            return ;
        }
        updateTextView("Sending ice candidate");
        Log.v(TAG, "Sending: " + message.toString());
        socket.send(message.toString());
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] ıceCandidates) {
        updateTextView("onIceCandidateRemoved");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        updateTextView("onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        updateTextView("onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        updateTextView("onDataChannel: " + dataChannel.hashCode());
        dataChannel.registerObserver(this);
    }

    @Override
    public void onRenegotiationNeeded() {
        updateTextView("onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        updateTextView("onAddTrack");
    }

    @Override
    public void onBufferedAmountChange(long l) {
        updateTextView("onBufferedAmountChange");
    }

    @Override
    public void onStateChange() {
        updateTextView("onStateChange");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        updateTextView("onMessage");
        StringBuilder s = new StringBuilder();
        for (int i=0; i<buffer.data.limit(); i++) {
            s.append(buffer.data.get(i));
            s.append(" ");
        }
        Log.v(TAG, "Message is: " + s.toString());
    }
}

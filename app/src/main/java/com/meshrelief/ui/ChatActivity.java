package com.meshrelief.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.meshrelief.R;
import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.transport.Transport;
import com.meshrelief.core.transport.WifiDirectTransport;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity implements WifiDirectTransport.PacketListener {
    private TextView statusText;
    private ListView messageList;
    private EditText messageInput;
    private Button sendButton;
    private Button peerListButton;
    private Button networkMapButton;

    private ArrayAdapter<String> messageAdapter;
    private List<String> messages = new ArrayList<>();

    private PeerManager peerManager;
    private WifiDirectTransport transport;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize views
        statusText = findViewById(R.id.statusText);
        messageList = findViewById(R.id.messageList);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        peerListButton = findViewById(R.id.peerListButton);
        networkMapButton = findViewById(R.id.networkMapButton);

        // Setup message list
        messageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(messageAdapter);

        // Initialize peer manager
        peerManager = new PeerManager();

        // Initialize WiFi Direct
        initializeWiFiDirect();

        // Setup button listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        peerListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, PeerListActivity.class));
            }
        });

        networkMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, NetworkMapActivity.class));
            }
        });

        addMessage("MeshRelief Chat Started");
        updateStatus("Initializing WiFi Direct...");
    }

    private void initializeWiFiDirect() {
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            updateStatus("WiFi Direct not supported");
            return;
        }

        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        transport = new WifiDirectTransport(this, wifiP2pManager, channel, peerManager);
        transport.setPacketListener(this);

        try {
            transport.start();
            updateStatus("WiFi Direct transport started");
        } catch (Exception e) {
            updateStatus("Failed to start transport: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        // Get first available peer
        List<Peer> peers = transport.getConnectedPeers();
        if (peers.isEmpty()) {
            Toast.makeText(this, "No connected peers", Toast.LENGTH_SHORT).show();
            return;
        }

        Peer targetPeer = peers.get(0);
        Packet packet = new Packet(
            "msg-" + System.currentTimeMillis(),
            "local-device", // TODO: get actual device ID
            targetPeer.getId(),
            10, // TTL
            System.currentTimeMillis(),
            text.getBytes()
        );

        transport.send(packet, targetPeer);
        addMessage("Sent: " + text);
        messageInput.setText("");
    }

    @Override
    public void onPacketReceived(Packet packet, String fromPeerId) {
        String message = new String(packet.getPayload());
        runOnUiThread(() -> addMessage("From " + fromPeerId + ": " + message));
    }

    private void addMessage(String message) {
        messages.add(message);
        messageAdapter.notifyDataSetChanged();
        messageList.setSelection(messages.size() - 1);
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> statusText.setText("Status: " + status));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transport != null) {
            transport.stop();
        }
    }
}

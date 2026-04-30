package com.meshrelief.core.transport;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.SerializationException;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerManager;
import com.meshrelief.core.p2p.PeerStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WiFi Direct Transport implementation.
 * Integrates all WiFi Direct components to provide transport layer functionality.
 * Requires Android context and WifiP2pManager.
 */
public class WifiDirectTransport implements Transport, WiFiDirectSocketHandler.PacketReceiverListener {
    private final Context context;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final PeerManager peerManager;

    private WiFiDirectManager wifiDirectManager;
    private WiFiDirectSocketHandler socketHandler;
    private PacketListener packetListener;
    private volatile boolean isRunning = false;

    /**
     * Interface for handling received packets from network.
     */
    public interface PacketListener {
        void onPacketReceived(Packet packet, String fromPeerId);
    }

    /**
     * Creates a WiFiDirectTransport instance.
     *
     * @param context Android application context
     * @param wifiP2pManager WiFiP2pManager instance
     * @param channel WiFi P2P communication channel
     * @param peerManager peer manager for tracking peers
     */
    public WifiDirectTransport(Context context, WifiP2pManager wifiP2pManager, 
                              WifiP2pManager.Channel channel, PeerManager peerManager) {
        if (context == null || wifiP2pManager == null || channel == null || peerManager == null) {
            throw new IllegalArgumentException("All parameters cannot be null");
        }
        this.context = context;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.peerManager = peerManager;
    }

    /**
     * Sets the packet listener for received packets.
     *
     * @param listener callback for received packets
     */
    public void setPacketListener(PacketListener listener) {
        this.packetListener = listener;
    }

    /**
     * Starts the WiFi Direct transport.
     * Initializes managers and starts discovery.
     *
     * @throws WiFiDirectManager.WiFiDirectException if startup fails
     */
    @Override
    public void start() throws WiFiDirectManager.WiFiDirectException {
        if (isRunning) {
            return; // Already running
        }

        try {
            // Initialize WiFi Direct manager
            wifiDirectManager = new WiFiDirectManager(context, wifiP2pManager, channel);
            wifiDirectManager.initialize();

            // Initialize socket handler
            socketHandler = new WiFiDirectSocketHandler(this);
            socketHandler.startServer();

            // Start peer discovery
            wifiDirectManager.startDiscovery();

            isRunning = true;
            System.out.println("WiFiDirectTransport started successfully");

        } catch (Exception e) {
            cleanup();
            if (e instanceof WiFiDirectManager.WiFiDirectException) {
                throw (WiFiDirectManager.WiFiDirectException) e;
            }
            throw new WiFiDirectManager.WiFiDirectException("Failed to start transport: " + e.getMessage(), e);
        }
    }

    /**
     * Stops the WiFi Direct transport.
     * Closes connections and releases resources.
     */
    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }

        cleanup();
        isRunning = false;
        System.out.println("WiFiDirectTransport stopped");
    }

    /**
     * Sends a packet to a peer over WiFi Direct.
     *
     * @param packet the packet to send
     * @param peer the target peer
     * @throws WiFiDirectManager.WiFiDirectException if send fails
     */
    @Override
    public void send(Packet packet, Peer peer) throws WiFiDirectManager.WiFiDirectException {
        if (!isRunning) {
            throw new WiFiDirectManager.WiFiDirectException("Transport not running");
        }

        if (packet == null || peer == null) {
            throw new IllegalArgumentException("Packet and Peer cannot be null");
        }

        try {
            // Get peer's IP address
            String peerIp = wifiDirectManager.getPeerIpAddress(peer.getId());
            if (peerIp == null || peerIp.isEmpty()) {
                throw new WiFiDirectManager.WiFiDirectException("Peer IP address unknown: " + peer.getId());
            }

            // Connect if not already connected
            if (!socketHandler.isConnected(peerIp)) {
                socketHandler.connectToPeer(peerIp);
            }

            // Send packet
            socketHandler.sendPacket(packet, peerIp);
            System.out.println("Packet sent to " + peer.getName() + " (" + peerIp + ")");

        } catch (SerializationException e) {
            throw new WiFiDirectManager.WiFiDirectException("Packet serialization failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new WiFiDirectManager.WiFiDirectException("Network error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WiFiDirectManager.WiFiDirectException("Failed to send packet: " + e.getMessage(), e);
        }
    }

    /**
     * Gets list of currently connected peers.
     *
     * @return list of peers with ACTIVE status
     */
    @Override
    public List<Peer> getConnectedPeers() {
        if (!isRunning || peerManager == null) {
            return new ArrayList<>();
        }
        return peerManager.getActivePeers();
    }

    /**
     * Gets discovered peers through WiFi Direct.
     *
     * @return list of discovered peers
     */
    public List<Peer> getDiscoveredPeers() {
        if (!isRunning || wifiDirectManager == null) {
            return new ArrayList<>();
        }
        return wifiDirectManager.getDiscoveredPeers();
    }

    /**
     * Connects to a discovered peer.
     *
     * @param peerId the peer ID (MAC address)
     * @throws WiFiDirectManager.WiFiDirectException if connection fails
     */
    public void connectToPeer(String peerId) throws WiFiDirectManager.WiFiDirectException {
        if (!isRunning || wifiDirectManager == null) {
            throw new WiFiDirectManager.WiFiDirectException("Transport not running");
        }

        try {
            wifiDirectManager.connectToPeer(peerId);
            peerManager.updatePeerStatus(peerId, PeerStatus.ACTIVE);

        } catch (Exception e) {
            if (e instanceof PeerManager.PeerException) {
                System.err.println("Peer not found in manager: " + peerId);
            }
            throw new WiFiDirectManager.WiFiDirectException("Failed to connect to peer: " + e.getMessage(), e);
        }
    }

    /**
     * Callback when a packet is received from network.
     * Delegates to registered packet listener.
     *
     * @param packet the received packet
     * @param fromIp source IP address
     */
    @Override
    public void onPacketReceived(Packet packet, String fromIp) {
        if (packetListener != null && packet != null) {
            // Find peer by IP to get peer ID
            String peerId = findPeerIdByIp(fromIp);
            packetListener.onPacketReceived(packet, peerId != null ? peerId : fromIp);
        }
    }

    /**
     * Callback when connection error occurs.
     *
     * @param ip IP address of the peer
     * @param error error message
     */
    @Override
    public void onConnectionError(String ip, String error) {
        System.err.println("Connection error with " + ip + ": " + error);
        // Could implement retry logic here
    }

    /**
     * Checks if transport is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets the WiFi Direct manager.
     *
     * @return WiFiDirectManager instance
     */
    public WiFiDirectManager getWifiDirectManager() {
        return wifiDirectManager;
    }

    /**
     * Gets the socket handler.
     *
     * @return WiFiDirectSocketHandler instance
     */
    public WiFiDirectSocketHandler getSocketHandler() {
        return socketHandler;
    }

    /**
     * Finds peer ID by IP address.
     *
     * @param ip IP address to search for
     * @return peer ID if found, null otherwise
     */
    private String findPeerIdByIp(String ip) {
        if (wifiDirectManager == null || peerManager == null) {
            return null;
        }

        for (Peer peer : peerManager.getAllPeers().values()) {
            String peerIp = wifiDirectManager.getPeerIpAddress(peer.getId());
            if (ip.equals(peerIp)) {
                return peer.getId();
            }
        }
        return null;
    }

    /**
     * Cleans up resources.
     */
    private void cleanup() {
        try {
            if (socketHandler != null) {
                socketHandler.cleanup();
            }

            if (wifiDirectManager != null) {
                wifiDirectManager.cleanup();
            }

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}

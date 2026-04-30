package com.meshrelief.core.transport;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.p2p.PeerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * WiFi Direct Manager - Orchestrates WiFi P2P operations.
 * Handles initialization, peer discovery, and connection management.
 * Thread-safe and requires Android permissions: CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION.
 */
public class WiFiDirectManager {
    private final Context context;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    
    private boolean isInitialized = false;
    private boolean wifiP2pEnabled = false;
    private boolean isDiscoveryActive = false;
    
    private final ConcurrentHashMap<String, WifiP2pDevice> discoveredDevices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceToIpMap = new ConcurrentHashMap<>();
    
    private WiFiDirectConnectionManager connectionManager;
    private WiFiDirectDiscovery discoveryManager;
    private WiFiDirectBroadcastReceiver broadcastReceiver;

    /**
     * Creates a WiFiDirectManager instance.
     *
     * @param context Android application context
     * @param wifiP2pManager WiFiP2pManager instance
     * @param channel communication channel for WiFi P2P operations
     */
    public WiFiDirectManager(Context context, WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        if (context == null || wifiP2pManager == null || channel == null) {
            throw new IllegalArgumentException("Context, WifiP2pManager, and Channel cannot be null");
        }
        this.context = context;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
    }

    /**
     * Initializes the WiFi Direct manager.
     * Must be called before any WiFi Direct operations.
     *
     * @throws WiFiDirectException if initialization fails
     */
    public synchronized void initialize() throws WiFiDirectException {
        if (isInitialized) {
            return; // Already initialized
        }

        try {
            // Verify WiFi P2P is supported
            if (!isWifiP2pSupported()) {
                throw new WiFiDirectException("WiFi Direct (WiFi P2P) is not supported on this device");
            }

            // Initialize connection manager
            connectionManager = new WiFiDirectConnectionManager(wifiP2pManager, channel);

            // Initialize discovery manager
            discoveryManager = new WiFiDirectDiscovery(wifiP2pManager, channel);

            // Initialize and register broadcast receiver
            broadcastReceiver = new WiFiDirectBroadcastReceiver(this, discoveryManager, connectionManager);
            broadcastReceiver.register(context);

            isInitialized = true;
            System.out.println("WiFiDirectManager initialized successfully");

        } catch (WiFiDirectException e) {
            throw e;
        } catch (Exception e) {
            throw new WiFiDirectException("Failed to initialize WiFi Direct: " + e.getMessage(), e);
        }
    }

    /**
     * Starts WiFi Direct peer discovery.
     *
     * @throws WiFiDirectException if discovery cannot start
     */
    public synchronized void startDiscovery() throws WiFiDirectException {
        if (!isInitialized) {
            throw new WiFiDirectException("WiFiDirectManager not initialized. Call initialize() first.");
        }

        if (isDiscoveryActive) {
            return; // Already discovering
        }

        try {
            discoveryManager.startDiscovery();
            isDiscoveryActive = true;
            System.out.println("WiFi Direct discovery started");

        } catch (Exception e) {
            throw new WiFiDirectException("Failed to start discovery: " + e.getMessage(), e);
        }
    }

    /**
     * Stops WiFi Direct peer discovery.
     */
    public synchronized void stopDiscovery() {
        if (!isInitialized || !isDiscoveryActive) {
            return;
        }

        try {
            discoveryManager.stopDiscovery();
            isDiscoveryActive = false;
            System.out.println("WiFi Direct discovery stopped");

        } catch (Exception e) {
            System.err.println("Error stopping discovery: " + e.getMessage());
        }
    }

    /**
     * Gets discovered peers.
     *
     * @return list of discovered peers
     */
    public List<Peer> getDiscoveredPeers() {
        return discoveryManager != null ? discoveryManager.getDiscoveredPeers() : new ArrayList<>();
    }

    /**
     * Connects to a specific WiFi Direct peer.
     *
     * @param deviceAddress MAC address of the device
     * @throws WiFiDirectException if connection fails
     */
    public void connectToPeer(String deviceAddress) throws WiFiDirectException {
        if (!isInitialized) {
            throw new WiFiDirectException("WiFiDirectManager not initialized");
        }

        if (connectionManager == null) {
            throw new WiFiDirectException("Connection manager not initialized");
        }

        try {
            connectionManager.connect(deviceAddress);
            System.out.println("Connecting to peer: " + deviceAddress);

        } catch (Exception e) {
            throw new WiFiDirectException("Failed to connect to peer: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects from a WiFi Direct peer.
     *
     * @param deviceAddress MAC address of the device (optional for disconnect)
     * @throws WiFiDirectException if disconnection fails
     */
    public void disconnect(String deviceAddress) throws WiFiDirectException {
        if (!isInitialized || connectionManager == null) {
            throw new WiFiDirectException("WiFiDirectManager not initialized");
        }

        try {
            connectionManager.disconnect();
            deviceToIpMap.remove(deviceAddress);
            System.out.println("Disconnected from peer: " + deviceAddress);

        } catch (Exception e) {
            throw new WiFiDirectException("Failed to disconnect: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the IP address of a connected peer.
     *
     * @param deviceAddress MAC address of the device
     * @return IP address, or null if not connected
     */
    public String getPeerIpAddress(String deviceAddress) {
        return deviceToIpMap.get(deviceAddress);
    }

    /**
     * Updates peer IP address mapping.
     * Called internally by connection manager when connection is established.
     *
     * @param deviceAddress MAC address
     * @param ipAddress IP address
     */
    public void setPeerIpAddress(String deviceAddress, String ipAddress) {
        deviceToIpMap.put(deviceAddress, ipAddress);
    }

    /**
     * Updates WiFi P2P enabled state.
     * Called by broadcast receiver when WiFi Direct state changes.
     *
     * @param enabled true if WiFi Direct is enabled
     */
    public void setWifiP2pEnabled(boolean enabled) {
        this.wifiP2pEnabled = enabled;
        System.out.println("WiFi Direct " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Checks if WiFi Direct is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isWifiP2pEnabled() {
        return wifiP2pEnabled;
    }

    /**
     * Checks if WiFi P2P is supported on this device.
     *
     * @return true if supported
     */
    private boolean isWifiP2pSupported() {
        try {
            WifiP2pDevice device = new WifiP2pDevice();
            return device != null;
        } catch (Exception e) {
            System.err.println("WiFi P2P support check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets connection info (Group Owner status, IP, etc.)
     *
     * @return WifiP2pInfo or null if not connected
     */
    public WifiP2pInfo getConnectionInfo() {
        return connectionManager != null ? connectionManager.getConnectionInfo() : null;
    }

    /**
     * Checks if currently in a WiFi Direct group.
     *
     * @return true if in group
     */
    public boolean isInGroup() {
        WifiP2pInfo info = getConnectionInfo();
        return info != null && info.groupFormed;
    }

    /**
     * Cleans up and releases resources.
     */
    public synchronized void cleanup() {
        try {
            stopDiscovery();

            if (broadcastReceiver != null) {
                broadcastReceiver.unregister(context);
            }

            if (connectionManager != null) {
                connectionManager.removeGroup();
            }

            discoveredDevices.clear();
            deviceToIpMap.clear();

            isInitialized = false;
            System.out.println("WiFiDirectManager cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    /**
     * Custom exception for WiFi Direct operations.
     */
    public static class WiFiDirectException extends Exception {
        public WiFiDirectException(String message) {
            super(message);
        }

        public WiFiDirectException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

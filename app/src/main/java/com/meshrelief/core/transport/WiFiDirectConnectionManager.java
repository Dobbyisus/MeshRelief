package com.meshrelief.core.transport;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * WiFi Direct Connection Manager.
 * Handles peer connections and group management.
 */
public class WiFiDirectConnectionManager {
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    
    private WifiP2pInfo connectionInfo;
    private String connectedPeerId;

    /**
     * Creates a WiFiDirectConnectionManager instance.
     *
     * @param wifiP2pManager WiFiP2pManager for WiFi Direct operations
     * @param channel communication channel
     */
    public WiFiDirectConnectionManager(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        if (wifiP2pManager == null || channel == null) {
            throw new IllegalArgumentException("WifiP2pManager and Channel cannot be null");
        }
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
    }

    /**
     * Initiates a connection to a WiFi Direct peer.
     *
     * @param deviceAddress MAC address of the peer device
     */
    public void connect(String deviceAddress) {
        if (deviceAddress == null || deviceAddress.isEmpty()) {
            throw new IllegalArgumentException("Device address cannot be null or empty");
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = android.net.wifi.p2p.WpsInfo.PBC; // Push Button Config

        try {
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    connectedPeerId = deviceAddress;
                    System.out.println("Connection initiated to: " + deviceAddress);
                }

                @Override
                public void onFailure(int reason) {
                    String reasonStr = getFailureReason(reason);
                    System.err.println("Connection failed: " + reasonStr);
                }
            });

        } catch (Exception e) {
            System.err.println("Error initiating connection: " + e.getMessage());
        }
    }

    /**
     * Disconnects from a WiFi Direct peer.
     */
    public void disconnect() {
        try {
            wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    connectedPeerId = null;
                    System.out.println("Disconnected from peer");
                }

                @Override
                public void onFailure(int reason) {
                    String reasonStr = getFailureReason(reason);
                    System.err.println("Disconnect failed: " + reasonStr);
                }
            });

        } catch (Exception e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Removes the WiFi Direct group (Group Owner operation).
     */
    public void removeGroup() {
        try {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    System.out.println("WiFi Direct group removed");
                }

                @Override
                public void onFailure(int reason) {
                    String reasonStr = getFailureReason(reason);
                    System.err.println("Group removal failed: " + reasonStr);
                }
            });

        } catch (Exception e) {
            System.err.println("Error removing group: " + e.getMessage());
        }
    }

    /**
     * Gets current connection information.
     *
     * @return WifiP2pInfo or null if not connected
     */
    public WifiP2pInfo getConnectionInfo() {
        return connectionInfo;
    }

    /**
     * Updates connection information.
     * Called by broadcast receiver when connection state changes.
     *
     * @param info the new connection information
     */
    public void setConnectionInfo(WifiP2pInfo info) {
        this.connectionInfo = info;
        if (info != null && info.groupFormed) {
            System.out.println("Group formed - Group Owner: " + (info.isGroupOwner ? "Yes" : "No") +
                    ", IP: " + info.groupOwnerAddress.getHostAddress());
        }
    }

    /**
     * Gets the ID of the connected peer.
     *
     * @return peer ID or null if not connected
     */
    public String getConnectedPeerId() {
        return connectedPeerId;
    }

    /**
     * Checks if currently connected to a peer.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connectionInfo != null && connectionInfo.groupFormed;
    }

    /**
     * Helper method to convert WiFi Direct error codes to readable strings.
     */
    private String getFailureReason(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P is not supported";
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.BUSY:
                return "Framework is busy";
            default:
                return "Unknown error (" + reason + ")";
        }
    }
}

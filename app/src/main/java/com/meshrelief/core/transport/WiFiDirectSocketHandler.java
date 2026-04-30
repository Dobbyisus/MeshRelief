package com.meshrelief.core.transport;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketSerializer;
import com.meshrelief.core.model.SerializationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WiFi Direct Socket Handler.
 * Manages TCP socket connections and packet I/O over WiFi Direct.
 * Handles serialization/deserialization of packets.
 */
public class WiFiDirectSocketHandler {
    private static final int LISTENING_PORT = 8888;
    private static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    private static final int THREAD_POOL_SIZE = 5;

    private final ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DataOutputStream> outputStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DataInputStream> inputStreams = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private PacketReceiverListener receiverListener;
    private volatile boolean isRunning = false;

    /**
     * Interface for receiving packets.
     */
    public interface PacketReceiverListener {
        void onPacketReceived(Packet packet, String fromIp);
        void onConnectionError(String ip, String error);
    }

    /**
     * Creates a WiFiDirectSocketHandler instance.
     *
     * @param listener callback for received packets
     */
    public WiFiDirectSocketHandler(PacketReceiverListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("PacketReceiverListener cannot be null");
        }
        this.receiverListener = listener;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    /**
     * Starts the socket server for receiving incoming connections.
     *
     * @throws IOException if server socket cannot be created
     */
    public synchronized void startServer() throws IOException {
        if (isRunning) {
            return; // Already running
        }

        try {
            serverSocket = new ServerSocket(LISTENING_PORT);
            serverSocket.setReuseAddress(true);
            isRunning = true;

            // Start accepting connections in background thread
            executorService.execute(this::acceptConnections);
            System.out.println("WiFi Direct socket server started on port " + LISTENING_PORT);

        } catch (IOException e) {
            throw new IOException("Failed to start socket server: " + e.getMessage(), e);
        }
    }

    /**
     * Stops the socket server and closes all connections.
     */
    public synchronized void stopServer() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        try {
            // Close all active sockets
            sockets.forEach((ip, socket) -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            });
            sockets.clear();
            outputStreams.clear();
            inputStreams.clear();

            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println("WiFi Direct socket server stopped");

        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Creates a client socket connection to a peer.
     *
     * @param ipAddress IP address of the peer
     * @return true if connection successful
     */
    public boolean connectToPeer(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        try {
            if (sockets.containsKey(ipAddress)) {
                return true; // Already connected
            }

            Socket socket = new Socket(ipAddress, LISTENING_PORT);
            socket.setSoTimeout(SOCKET_TIMEOUT);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            sockets.put(ipAddress, socket);
            outputStreams.put(ipAddress, out);
            inputStreams.put(ipAddress, in);

            // Start listening for incoming messages from this peer
            executorService.execute(() -> listenForMessages(ipAddress, in));

            System.out.println("Connected to peer at " + ipAddress);
            return true;

        } catch (IOException e) {
            System.err.println("Failed to connect to peer " + ipAddress + ": " + e.getMessage());
            receiverListener.onConnectionError(ipAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a packet to a connected peer.
     *
     * @param packet the packet to send
     * @param ipAddress IP address of the target peer
     * @throws IOException if send fails
     * @throws SerializationException if packet serialization fails
     */
    public void sendPacket(Packet packet, String ipAddress) throws IOException, SerializationException {
        if (packet == null || ipAddress == null || ipAddress.isEmpty()) {
            throw new IllegalArgumentException("Packet and IP address cannot be null or empty");
        }

        DataOutputStream out = outputStreams.get(ipAddress);
        if (out == null) {
            throw new IOException("Not connected to peer: " + ipAddress);
        }

        try {
            byte[] serialized = PacketSerializer.serialize(packet);
            synchronized (out) {
                out.writeInt(serialized.length);
                out.write(serialized);
                out.flush();
            }

            System.out.println("Packet sent to " + ipAddress + " (size: " + serialized.length + " bytes)");

        } catch (IOException e) {
            // Connection lost, remove socket
            disconnectFromPeer(ipAddress);
            throw new IOException("Failed to send packet: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects from a peer.
     *
     * @param ipAddress IP address of the peer
     */
    public void disconnectFromPeer(String ipAddress) {
        try {
            Socket socket = sockets.remove(ipAddress);
            outputStreams.remove(ipAddress);
            inputStreams.remove(ipAddress);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            System.out.println("Disconnected from peer: " + ipAddress);

        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Checks if connected to a peer.
     *
     * @param ipAddress IP address of the peer
     * @return true if connected
     */
    public boolean isConnected(String ipAddress) {
        Socket socket = sockets.get(ipAddress);
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Accepts incoming socket connections (runs in background thread).
     */
    private void acceptConnections() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();

                System.out.println("Incoming connection from: " + clientIp);

                // Store the socket
                sockets.put(clientIp, clientSocket);
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                outputStreams.put(clientIp, out);
                inputStreams.put(clientIp, in);

                // Listen for messages from this client
                executorService.execute(() -> listenForMessages(clientIp, in));

            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Listens for incoming packets from a peer (runs in background thread).
     *
     * @param ipAddress IP address of the peer
     * @param input data stream to read from
     */
    private void listenForMessages(String ipAddress, DataInputStream input) {
        while (isRunning && isConnected(ipAddress)) {
            try {
                // Read packet length
                int packetLength = input.readInt();
                if (packetLength <= 0) {
                    throw new IOException("Invalid packet length: " + packetLength);
                }

                // Read packet data
                byte[] packetData = new byte[packetLength];
                input.readFully(packetData);

                // Deserialize packet
                try {
                    Packet packet = PacketSerializer.deserialize(packetData);
                    receiverListener.onPacketReceived(packet, ipAddress);

                } catch (SerializationException e) {
                    System.err.println("Failed to deserialize packet from " + ipAddress + ": " + e.getMessage());
                }

            } catch (IOException e) {
                System.err.println("Connection lost with peer " + ipAddress + ": " + e.getMessage());
                disconnectFromPeer(ipAddress);
                receiverListener.onConnectionError(ipAddress, "Connection lost: " + e.getMessage());
            }
        }
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        stopServer();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

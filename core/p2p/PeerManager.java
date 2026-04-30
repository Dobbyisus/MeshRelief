package core.p2p;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Robust peer management system for mesh networks.
 * Thread-safe, with support for peer lifecycle management, notifications, and advanced queries.
 */
public class PeerManager {
    private static final long DEFAULT_INACTIVITY_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    private static final long DEFAULT_DISCOVERY_TIMEOUT = 30 * 1000; // 30 seconds

    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final List<PeerListener> listeners = new CopyOnWriteArrayList<>();
    private final long inactivityTimeout;
    private final long discoveryTimeout;

    /**
     * Creates a PeerManager with default timeout settings.
     */
    public PeerManager() {
        this(DEFAULT_INACTIVITY_TIMEOUT, DEFAULT_DISCOVERY_TIMEOUT);
    }

    /**
     * Creates a PeerManager with custom timeout settings.
     *
     * @param inactivityTimeout time in milliseconds before marking a peer as inactive
     * @param discoveryTimeout time in milliseconds before removing undiscovered peers
     */
    public PeerManager(long inactivityTimeout, long discoveryTimeout) {
        if (inactivityTimeout <= 0 || discoveryTimeout <= 0) {
            throw new IllegalArgumentException("Timeouts must be positive values");
        }
        this.inactivityTimeout = inactivityTimeout;
        this.discoveryTimeout = discoveryTimeout;
    }

    /**
     * Adds a new peer to the network.
     *
     * @param peer the peer to add
     * @throws IllegalArgumentException if peer is null
     * @throws PeerException if peer with same ID already exists
     */
    public synchronized void addPeer(Peer peer) throws PeerException {
        if (peer == null) {
            throw new IllegalArgumentException("Peer cannot be null");
        }

        if (peers.containsKey(peer.getId())) {
            throw new PeerException("Peer with ID '" + peer.getId() + "' already exists");
        }

        peers.put(peer.getId(), peer);
        notifyPeerAdded(peer);
    }

    /**
     * Retrieves a peer by ID.
     *
     * @param id the peer ID
     * @return the peer, or null if not found
     * @throws IllegalArgumentException if id is null or empty
     */
    public Peer getPeer(String id) {
        validateId(id);
        return peers.get(id);
    }

    /**
     * Checks if a peer exists.
     *
     * @param id the peer ID
     * @return true if peer exists, false otherwise
     * @throws IllegalArgumentException if id is null or empty
     */
    public boolean hasPeer(String id) {
        validateId(id);
        return peers.containsKey(id);
    }

    /**
     * Gets all peers as an unmodifiable map.
     *
     * @return unmodifiable map of all peers
     */
    public Map<String, Peer> getAllPeers() {
        return Collections.unmodifiableMap(new HashMap<>(peers));
    }

    /**
     * Gets the total number of peers.
     *
     * @return number of peers
     */
    public int getPeerCount() {
        return peers.size();
    }

    /**
     * Gets the number of active peers.
     *
     * @return number of peers with ACTIVE status
     */
    public int getActivePeerCount() {
        return (int) peers.values().stream()
                .filter(Peer::isActive)
                .count();
    }

    /**
     * Removes a peer from the network.
     *
     * @param id the peer ID
     * @param reason the reason for removal
     * @return the removed peer, or null if not found
     * @throws IllegalArgumentException if id is null or empty
     */
    public synchronized Peer removePeer(String id, String reason) {
        validateId(id);
        Peer removed = peers.remove(id);
        if (removed != null) {
            notifyPeerRemoved(id, reason);
        }
        return removed;
    }

    /**
     * Removes a peer from the network with default reason.
     *
     * @param id the peer ID
     * @return the removed peer, or null if not found
     * @throws IllegalArgumentException if id is null or empty
     */
    public Peer removePeer(String id) {
        return removePeer(id, "Unknown");
    }

    /**
     * Updates a peer's status.
     *
     * @param id the peer ID
     * @param newStatus the new status
     * @throws IllegalArgumentException if id is null or empty, or status is null
     * @throws PeerException if peer not found
     */
    public synchronized void updatePeerStatus(String id, PeerStatus newStatus) throws PeerException {
        validateId(id);
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Peer peer = peers.get(id);
        if (peer == null) {
            throw new PeerException("Peer with ID '" + id + "' not found");
        }

        PeerStatus oldStatus = peer.getStatus();
        if (!oldStatus.equals(newStatus)) {
            Peer updated = peer.withStatus(newStatus);
            peers.put(id, updated);
            notifyPeerStatusChanged(id, newStatus, oldStatus);
        }
    }

    /**
     * Updates a peer's last seen time to now.
     *
     * @param id the peer ID
     * @throws IllegalArgumentException if id is null or empty
     * @throws PeerException if peer not found
     */
    public synchronized void markPeerActive(String id) throws PeerException {
        validateId(id);
        Peer peer = peers.get(id);
        if (peer == null) {
            throw new PeerException("Peer with ID '" + id + "' not found");
        }

        Peer updated = peer.withUpdatedLastSeen();
        if (!peer.getStatus().equals(PeerStatus.ACTIVE)) {
            updated = updated.withStatus(PeerStatus.ACTIVE);
            notifyPeerStatusChanged(id, PeerStatus.ACTIVE, peer.getStatus());
        }
        peers.put(id, updated);
    }

    /**
     * Marks a peer as inactive.
     *
     * @param id the peer ID
     * @throws IllegalArgumentException if id is null or empty
     * @throws PeerException if peer not found
     */
    public synchronized void markPeerInactive(String id) throws PeerException {
        validateId(id);
        updatePeerStatus(id, PeerStatus.INACTIVE);
    }

    /**
     * Gets all peers with a specific status.
     *
     * @param status the status to filter by
     * @return list of peers with the given status
     * @throws IllegalArgumentException if status is null
     */
    public List<Peer> getPeersByStatus(PeerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        return peers.values().stream()
                .filter(peer -> peer.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Gets all active peers.
     *
     * @return list of peers with ACTIVE status
     */
    public List<Peer> getActivePeers() {
        return getPeersByStatus(PeerStatus.ACTIVE);
    }

    /**
     * Gets all inactive peers.
     *
     * @return list of peers with INACTIVE status
     */
    public List<Peer> getInactivePeers() {
        return getPeersByStatus(PeerStatus.INACTIVE);
    }

    /**
     * Removes all peers that have been inactive for longer than the inactivity timeout.
     *
     * @return list of peer IDs that were removed
     */
    public synchronized List<String> clearInactivePeers() {
        List<String> removed = new ArrayList<>();
        peers.entrySet().removeIf(entry -> {
            if (entry.getValue().getTimeSinceLastSeen() > inactivityTimeout) {
                notifyPeerRemoved(entry.getKey(), "Inactivity timeout");
                removed.add(entry.getKey());
                return true;
            }
            return false;
        });
        return removed;
    }

    /**
     * Removes all peers with DISCOVERED status that haven't been verified.
     *
     * @return list of peer IDs that were removed
     */
    public synchronized List<String> clearUnverifiedDiscoveries() {
        List<String> removed = new ArrayList<>();
        peers.entrySet().removeIf(entry -> {
            Peer peer = entry.getValue();
            if (peer.getStatus() == PeerStatus.DISCOVERED && 
                peer.getTimeSinceLastSeen() > discoveryTimeout) {
                notifyPeerRemoved(entry.getKey(), "Discovery verification timeout");
                removed.add(entry.getKey());
                return true;
            }
            return false;
        });
        return removed;
    }

    /**
     * Gets the nearest peers sorted by signal strength.
     *
     * @param count the maximum number of peers to return
     * @return list of nearest peers
     * @throws IllegalArgumentException if count is less than 1
     */
    public List<Peer> getNearestPeers(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Count must be at least 1");
        }

        return peers.values().stream()
                .filter(peer -> peer.getSignalStrength() >= 0)
                .sorted((p1, p2) -> Double.compare(p2.getSignalStrength(), p1.getSignalStrength()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Gets peers within a specific signal strength range.
     *
     * @param minStrength minimum signal strength
     * @param maxStrength maximum signal strength
     * @return list of peers within the range
     */
    public List<Peer> getPeersBySignalStrength(double minStrength, double maxStrength) {
        if (minStrength > maxStrength) {
            throw new IllegalArgumentException("Minimum strength cannot be greater than maximum");
        }

        return peers.values().stream()
                .filter(peer -> peer.getSignalStrength() >= minStrength && 
                               peer.getSignalStrength() <= maxStrength)
                .collect(Collectors.toList());
    }

    /**
     * Adds a listener for peer management events.
     *
     * @param listener the listener to add
     * @throws IllegalArgumentException if listener is null
     */
    public void addListener(PeerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     * @return true if removed, false if not found
     */
    public boolean removeListener(PeerListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Gets the number of registered listeners.
     *
     * @return number of listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clears all peers and listeners.
     */
    public synchronized void clear() {
        peers.clear();
        listeners.clear();
    }

    // Private helper methods

    private void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Peer ID cannot be null or empty");
        }
    }

    private void notifyPeerAdded(Peer peer) {
        listeners.forEach(listener -> {
            try {
                listener.onPeerAdded(peer);
            } catch (Exception e) {
                System.err.println("Error notifying listener of peer added: " + e.getMessage());
            }
        });
    }

    private void notifyPeerRemoved(String peerId, String reason) {
        listeners.forEach(listener -> {
            try {
                listener.onPeerRemoved(peerId, reason);
            } catch (Exception e) {
                System.err.println("Error notifying listener of peer removed: " + e.getMessage());
            }
        });
    }

    private void notifyPeerStatusChanged(String peerId, PeerStatus newStatus, PeerStatus oldStatus) {
        listeners.forEach(listener -> {
            try {
                listener.onPeerStatusChanged(peerId, newStatus, oldStatus);
            } catch (Exception e) {
                System.err.println("Error notifying listener of peer status changed: " + e.getMessage());
            }
        });
    }

    private void notifyPeerUpdated(Peer peer) {
        listeners.forEach(listener -> {
            try {
                listener.onPeerUpdated(peer);
            } catch (Exception e) {
                System.err.println("Error notifying listener of peer updated: " + e.getMessage());
            }
        });
    }

    /**
     * Custom exception for peer management errors.
     */
    public static class PeerException extends Exception {
        public PeerException(String message) {
            super(message);
        }

        public PeerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

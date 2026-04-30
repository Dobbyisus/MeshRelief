package core.p2p;

import java.util.Objects;

/**
 * Represents a peer in the mesh network.
 * Immutable class with status tracking and metadata.
 */
public class Peer {
    private final String id;
    private final String name;
    private final PeerStatus status;
    private final long lastSeen;
    private final double signalStrength;
    private final String metadata;

    /**
     * Creates a new Peer with minimal information.
     *
     * @param id the unique identifier of the peer
     * @param name the human-readable name of the peer
     * @throws IllegalArgumentException if id or name is null or empty
     */
    public Peer(String id, String name) {
        this(id, name, PeerStatus.DISCOVERED, System.currentTimeMillis(), -1.0, null);
    }

    /**
     * Creates a new Peer with full information.
     *
     * @param id the unique identifier of the peer
     * @param name the human-readable name of the peer
     * @param status the current status of the peer
     * @param lastSeen timestamp of last communication (milliseconds)
     * @param signalStrength signal strength indicator (-1 if unknown)
     * @param metadata optional metadata about the peer
     * @throws IllegalArgumentException if id or name is null or empty
     */
    public Peer(String id, String name, PeerStatus status, long lastSeen, double signalStrength, String metadata) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Peer ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Peer name cannot be null or empty");
        }

        this.id = id;
        this.name = name;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.lastSeen = lastSeen;
        this.signalStrength = signalStrength;
        this.metadata = metadata;
    }

    /**
     * Creates a copy of this peer with updated status.
     *
     * @param newStatus the new status
     * @return a new Peer instance with the updated status
     */
    public Peer withStatus(PeerStatus newStatus) {
        return new Peer(this.id, this.name, newStatus, System.currentTimeMillis(), this.signalStrength, this.metadata);
    }

    /**
     * Creates a copy of this peer with updated last seen time.
     *
     * @return a new Peer instance with updated last seen time
     */
    public Peer withUpdatedLastSeen() {
        return new Peer(this.id, this.name, this.status, System.currentTimeMillis(), this.signalStrength, this.metadata);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PeerStatus getStatus() {
        return status;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public double getSignalStrength() {
        return signalStrength;
    }

    public String getMetadata() {
        return metadata;
    }

    /**
     * Checks if this peer is considered active.
     *
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == PeerStatus.ACTIVE;
    }

    /**
     * Calculates how long it has been since this peer was last seen (in milliseconds).
     *
     * @return time elapsed since last seen
     */
    public long getTimeSinceLastSeen() {
        return System.currentTimeMillis() - lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(id, peer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", lastSeen=" + lastSeen +
                ", signalStrength=" + signalStrength +
                '}';
    }
}

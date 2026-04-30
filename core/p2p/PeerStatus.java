package core.p2p;

/**
 * Enum representing the current status of a peer in the network.
 */
public enum PeerStatus {
    /**
     * Peer is actively connected and communicating.
     */
    ACTIVE,

    /**
     * Peer is known but temporarily unreachable.
     */
    INACTIVE,

    /**
     * Peer has been discovered but not yet verified.
     */
    DISCOVERED,

    /**
     * Peer connection was terminated or removed.
     */
    DISCONNECTED
}

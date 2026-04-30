package com.meshrelief.core.p2p;

/**
 * Interface for listening to peer management events.
 * Implementations will be notified when peers are added, removed, or updated.
 */
public interface PeerListener {
    /**
     * Called when a peer is added to the network.
     *
     * @param peer the peer that was added
     */
    void onPeerAdded(Peer peer);

    /**
     * Called when a peer is removed from the network.
     *
     * @param peerId the ID of the peer that was removed
     * @param reason the reason for removal
     */
    void onPeerRemoved(String peerId, String reason);

    /**
     * Called when a peer's status changes.
     *
     * @param peerId the ID of the peer
     * @param newStatus the new status
     * @param oldStatus the previous status
     */
    void onPeerStatusChanged(String peerId, PeerStatus newStatus, PeerStatus oldStatus);

    /**
     * Called when a peer's information is updated.
     *
     * @param peer the updated peer information
     */
    void onPeerUpdated(Peer peer);
}

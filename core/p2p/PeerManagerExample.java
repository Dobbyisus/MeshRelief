package core.p2p;

import java.util.List;

/**
 * Example usage of the robust PeerManager system.
 * Demonstrates all major features and best practices.
 */
public class PeerManagerExample {

    public static void main(String[] args) throws PeerManager.PeerException {
        System.out.println("=== Robust Peer Management System Demo ===\n");

        // Initialize PeerManager with custom timeouts
        PeerManager manager = new PeerManager(5 * 60 * 1000, 30 * 1000); // 5 min, 30 sec

        // Add a listener to track peer events
        SimplePeerListener listener = new SimplePeerListener();
        manager.addListener(listener);

        try {
            // Example 1: Adding peers
            System.out.println("1. Adding Peers:");
            Peer peer1 = new Peer("peer-001", "Alice", PeerStatus.ACTIVE, 
                    System.currentTimeMillis(), 85.5, "Location: North");
            Peer peer2 = new Peer("peer-002", "Bob");  // Uses default constructor
            Peer peer3 = new Peer("peer-003", "Charlie", PeerStatus.DISCOVERED, 
                    System.currentTimeMillis(), 45.2, null);

            manager.addPeer(peer1);
            manager.addPeer(peer2);
            manager.addPeer(peer3);

            System.out.println("✓ Added 3 peers. Total: " + manager.getPeerCount());
            System.out.println("✓ Active peers: " + manager.getActivePeerCount() + "\n");

            // Example 2: Retrieving peers
            System.out.println("2. Retrieving Peers:");
            System.out.println("✓ Get by ID: " + manager.getPeer("peer-001"));
            System.out.println("✓ Has peer: " + manager.hasPeer("peer-002"));
            System.out.println("✓ Peer count: " + manager.getPeerCount() + "\n");

            // Example 3: Updating peer status
            System.out.println("3. Managing Peer Status:");
            manager.updatePeerStatus("peer-002", PeerStatus.ACTIVE);
            System.out.println("✓ Updated peer-002 to ACTIVE");
            manager.markPeerInactive("peer-003");
            System.out.println("✓ Marked peer-003 as INACTIVE\n");

            // Example 4: Querying by status
            System.out.println("4. Querying Peers by Status:");
            System.out.println("✓ Active peers: " + manager.getActivePeers());
            System.out.println("✓ Inactive peers: " + manager.getInactivePeers() + "\n");

            // Example 5: Signal strength queries
            System.out.println("5. Signal Strength Queries:");
            List<Peer> nearest = manager.getNearestPeers(2);
            System.out.println("✓ 2 Nearest peers: " + nearest);
            
            List<Peer> inRange = manager.getPeersBySignalStrength(40.0, 90.0);
            System.out.println("✓ Peers with signal 40-90: " + inRange + "\n");

            // Example 6: Error handling - duplicate peer
            System.out.println("6. Error Handling:");
            try {
                manager.addPeer(new Peer("peer-001", "Duplicate"));
                System.out.println("✗ Should have thrown exception!");
            } catch (PeerManager.PeerException e) {
                System.out.println("✓ Caught expected exception: " + e.getMessage());
            }

            // Example 7: Removing peers
            System.out.println("\n7. Removing Peers:");
            manager.removePeer("peer-003", "No longer reachable");
            System.out.println("✓ Removed peer-003. Remaining: " + manager.getPeerCount() + "\n");

            // Example 8: Peer lifecycle
            System.out.println("8. Peer Lifecycle:");
            Peer retrieved = manager.getPeer("peer-001");
            System.out.println("✓ Last seen: " + retrieved.getLastSeen());
            System.out.println("✓ Time since last seen: " + retrieved.getTimeSinceLastSeen() + "ms");
            System.out.println("✓ Is active: " + retrieved.isActive() + "\n");

            // Example 9: Batch cleanup
            System.out.println("9. Peer Maintenance:");
            List<String> stale = manager.clearInactivePeers();
            System.out.println("✓ Cleared stale peers: " + stale);
            List<String> unverified = manager.clearUnverifiedDiscoveries();
            System.out.println("✓ Cleared unverified: " + unverified + "\n");

            // Example 10: View all peers
            System.out.println("10. All Peers:");
            manager.getAllPeers().values().forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Demo Complete ===");
    }

    /**
     * Simple listener implementation for demonstration.
     */
    static class SimplePeerListener implements PeerListener {
        @Override
        public void onPeerAdded(Peer peer) {
            System.out.println("  [EVENT] Peer added: " + peer.getId() + " (" + peer.getName() + ")");
        }

        @Override
        public void onPeerRemoved(String peerId, String reason) {
            System.out.println("  [EVENT] Peer removed: " + peerId + " (Reason: " + reason + ")");
        }

        @Override
        public void onPeerStatusChanged(String peerId, PeerStatus newStatus, PeerStatus oldStatus) {
            System.out.println("  [EVENT] Status changed: " + peerId + " (" + oldStatus + " → " + newStatus + ")");
        }

        @Override
        public void onPeerUpdated(Peer peer) {
            System.out.println("  [EVENT] Peer updated: " + peer.getId());
        }
    }
}

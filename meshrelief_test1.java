import core.model.Packet;
import core.p2p.Peer;
import core.routing.FloodRouter;
import core.store.SeenCache;
import core.transport.MockTransport;
// import java.util.stream.Collectors;

public class meshrelief_test1 {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("   MESHRELIEF FLOOD ROUTING TEST");
        System.out.println("========================================");
        System.out.println("Topology: A <---> B <---> C");
        System.out.println();

        // ===== SETUP PHASE =====
        System.out.println("[SETUP] Creating 3 nodes with MockTransport...");
        MockTransport transportA = new MockTransport("A");
        MockTransport transportB = new MockTransport("B");
        MockTransport transportC = new MockTransport("C");

        // Create seen caches (LRU with max 100 entries)
        SeenCache cacheA = new SeenCache(100);
        SeenCache cacheB = new SeenCache(100);
        SeenCache cacheC = new SeenCache(100);

        // Create routers
        FloodRouter routerA = new FloodRouter(cacheA, transportA);
        FloodRouter routerB = new FloodRouter(cacheB, transportB);
        FloodRouter routerC = new FloodRouter(cacheC, transportC);

        // Set routers in transports
        transportA.setRouter(routerA);
        transportB.setRouter(routerB);
        transportC.setRouter(routerC);

        // Connect nodes: A <-> B and B <-> C
        System.out.println("[SETUP] Connecting A <---> B");
        transportA.connectTo(transportB);
        System.out.println("[SETUP] Connecting B <---> C");
        transportB.connectTo(transportC);

        // Start all transports
        transportA.start();
        transportB.start();
        transportC.start();

        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST 1: Single Packet Flooding (A->B->C)");
        System.out.println("========================================");

        // Create and send a test packet from A with high TTL
        byte[] payload1 = "Message from A!".getBytes();
        Packet pkt1 = new Packet("msg-001", "A", null, 5, System.currentTimeMillis(), payload1);

        System.out.println("[TEST1] Node A initiates send of packet: " + pkt1.getPacketId());
        System.out.println();
        routerA.send(pkt1);

        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST 2: Duplicate Packet (Robustness Test)");
        System.out.println("========================================");
        System.out.println("[TEST2] Injecting DUPLICATE of msg-001 into Node B");
        System.out.println("[TEST2] -> This should be DROPPED by B's dedup logic");
        System.out.println();

        // Inject the same packet ID into B as if it came from C (duplicate)
        byte[] duplicatePayload = "Duplicate from C".getBytes();
        Packet pkt1Dup = new Packet("msg-001", "C", null, 4, System.currentTimeMillis(), duplicatePayload);
        transportB.receivePacket(pkt1Dup, new Peer("C", "NodeC"));

        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST 3: New Message from Different Source");
        System.out.println("========================================");

        // Create and send a new packet from C
        byte[] payload2 = "Message from C!".getBytes();
        Packet pkt2 = new Packet("msg-002", "C", null, 5, System.currentTimeMillis(), payload2);

        System.out.println("[TEST3] Node C initiates send of packet: " + pkt2.getPacketId());
        System.out.println();
        routerC.send(pkt2);

        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST 4: Multiple Duplicates in Stream");
        System.out.println("========================================");
        System.out.println("[TEST4] Injecting msg-001 again from A (should be dropped everywhere)");
        System.out.println();

        // Try to send msg-001 again from A
        byte[] payload1b = "Retry from A".getBytes();
        Packet pkt1Retry = new Packet("msg-001", "A", null, 5, System.currentTimeMillis(), payload1b);
        routerA.send(pkt1Retry);

        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST 5: TTL Expiration Test");
        System.out.println("========================================");
        System.out.println("[TEST5] Sending packet with TTL=1 (should stop at B, not reach C)");
        System.out.println();

        byte[] payload3 = "Low TTL message".getBytes();
        Packet pkt3 = new Packet("msg-003", "A", null, 1, System.currentTimeMillis(), payload3);
        routerA.send(pkt3);

        System.out.println();
        System.out.println("========================================");
        System.out.println("STATISTICS");
        System.out.println("========================================");
        System.out.println("Node A sent packets: " + transportA.getSentPackets().size());
        System.out.println("Node B sent packets: " + transportB.getSentPackets().size());
        System.out.println("Node C sent packets: " + transportC.getSentPackets().size());

        System.out.println();
        System.out.println("Details:");
        System.out.println("  A sent: " + transportA.getSentPackets().stream().map(p -> p.getPacketId()).collect(java.util.stream.Collectors.toList()));
        System.out.println("  B sent: " + transportB.getSentPackets().stream().map(p -> p.getPacketId()).collect(java.util.stream.Collectors.toList()));
        System.out.println("  C sent: " + transportC.getSentPackets().stream().map(p -> p.getPacketId()).collect(java.util.stream.Collectors.toList()));

        System.out.println();
        System.out.println("========================================");
        System.out.println("SHUTTING DOWN");
        System.out.println("========================================");
        transportA.stop();
        transportB.stop();
        transportC.stop();

        System.out.println();
        System.out.println("Test completed successfully!");
    }
}

package com.meshrelief.core.routing;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.store.SeenCache;
import com.meshrelief.core.transport.Transport;
// import com.meshrelief.core.routing.Router;
import java.util.List;

public class FloodRouter implements Router {

    private final SeenCache seenCache;
    private final Transport transport;

    public FloodRouter(SeenCache seenCache, Transport transport) {
        this.seenCache = seenCache;
        this.transport = transport;
    }

    // Called when a packet is received from network
    @Override
    public void onReceive(Packet packet, Peer from) {

        String packetId = packet.getPacketId();

        // 1. Drop duplicates
        if (seenCache.hasSeen(packetId)) {
            return;
        }

        // 2. Mark as seen
        seenCache.markSeen(packetId);

        // 3. Deliver locally (for now just log / later send to UI/store)
        deliver(packet);

        // 4. Forward if TTL > 0
        if (packet.getTtl() <= 0) {
            return;
        }

        // Decrement TTL
        packet.setTtl(packet.getTtl() - 1);

        // Forward to all peers except sender
        List<Peer> peers = transport.getConnectedPeers();

        for (Peer peer : peers) {
            if (from != null && peer.getId().equals(from.getId())) {
                continue; // skip sender
            }

            transport.send(packet, peer);
        }
    }

    // Called when THIS node wants to send a new message
    @Override
    public void send(Packet packet) {

        // Mark as seen so we don’t reprocess our own packet later
        seenCache.markSeen(packet.getPacketId());

        // Deliver locally
        deliver(packet);

        // Send to all peers
        List<Peer> peers = transport.getConnectedPeers();

        for (Peer peer : peers) {
            transport.send(packet, peer);
        }
    }

    // Local delivery (you’ll connect this to UI later)
    private void deliver(Packet packet) {
        System.out.println("Received packet: " + packet.getPacketId());
    }
}
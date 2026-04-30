package com.meshrelief.core.transport;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import com.meshrelief.core.routing.Router;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockTransport implements Transport {
    private final String nodeId;
    private final Map<String, MockTransport> connectedTransports = new HashMap<>();
    private final List<Packet> sentPackets = new ArrayList<>();
    private Router router;

    public MockTransport(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public void connectTo(MockTransport other) {
        connectedTransports.put(other.nodeId, other);
        other.connectedTransports.put(this.nodeId, this);
    }

    @Override
    public void start() {
        System.out.println("[" + nodeId + "] Transport started");
    }

    @Override
    public void stop() {
        System.out.println("[" + nodeId + "] Transport stopped");
    }

    @Override
    public void send(Packet packet, Peer peer) {
        sentPackets.add(packet);
        System.out.println("[" + nodeId + "] sending... packet " + packet.getPacketId() + " to peer " + peer.getId());

        // Simulate network delivery
        MockTransport targetTransport = connectedTransports.get(peer.getId());
        if (targetTransport != null && targetTransport.router != null) {
            Packet copy = packet.copy();//sending a copy instead of of the actual packet
            //prevents the ttl bug 
            targetTransport.receivePacket(copy, new Peer(nodeId, nodeId));
        }
    }

    public void receivePacket(Packet packet, Peer from) {
        System.out.println("[" + nodeId + "] receiving... packet " + packet.getPacketId() + " from " + from.getId());
        if (router != null) {
            router.onReceive(packet, from);
        }
    }

    @Override
    public List<Peer> getConnectedPeers() {
        List<Peer> peers = new ArrayList<>();
        for (String peerId : connectedTransports.keySet()) {
            peers.add(new Peer(peerId, peerId));
        }
        return peers;
    }

    public List<Packet> getSentPackets() {
        return sentPackets;
    }

    public String getNodeId() {
        return nodeId;
    }
}

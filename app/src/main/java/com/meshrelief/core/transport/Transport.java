package com.meshrelief.core.transport;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import java.util.List;

public interface Transport {
    void start();
    void stop();
    void send(Packet packet, Peer peer);
    List<Peer> getConnectedPeers();
}

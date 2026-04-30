package com.meshrelief.core.transport;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;
import java.util.Collections;
import java.util.List;

public class BluetoothTransport implements Transport {
    @Override
    public void start() {
        // TODO: initialize Bluetooth transport
    }

    @Override
    public void stop() {
        // TODO: release Bluetooth resources
    }

    @Override
    public void send(Packet packet, Peer peer) {
        // TODO: send packet to peer over Bluetooth
    }

    @Override
    public List<Peer> getConnectedPeers() {
        return Collections.emptyList();
    }
}

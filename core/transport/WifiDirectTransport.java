package core.transport;

import core.model.Packet;
import core.p2p.Peer;
import java.util.Collections;
import java.util.List;

public class WifiDirectTransport implements Transport {
    @Override
    public void start() {
        // TODO: initialize WiFi Direct transport
    }

    @Override
    public void stop() {
        // TODO: release WiFi Direct resources
    }

    @Override
    public void send(Packet packet, Peer peer) {
        // TODO: send packet to peer over WiFi Direct
    }

    @Override
    public List<Peer> getConnectedPeers() {
        return Collections.emptyList();
    }
}

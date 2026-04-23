package core.transport;

import core.model.Packet;
import core.p2p.Peer;
import java.util.List;

public interface Transport {
    void start();
    void stop();
    void send(Packet packet, Peer peer);
    List<Peer> getConnectedPeers();
}

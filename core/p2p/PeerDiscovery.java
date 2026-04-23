package core.p2p;

import java.util.List;

public interface PeerDiscovery {
    void startDiscovery();
    void stopDiscovery();
    List<Peer> getDiscoveredPeers();
}

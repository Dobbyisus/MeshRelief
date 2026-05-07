package com.meshrelief.core.p2p;

import java.util.List;

public interface PeerDiscovery {
    //checking issue with github pusht
    void startDiscovery();
    void stopDiscovery();
    List<Peer> getDiscoveredPeers();
}

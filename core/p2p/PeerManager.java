package core.p2p;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PeerManager {
    private final Map<String, Peer> peers = new HashMap<>();

    public void addPeer(Peer peer) {
        peers.put(peer.getId(), peer);
    }

    public Peer getPeer(String id) {
        return peers.get(id);
    }

    public Map<String, Peer> getAllPeers() {
        return Collections.unmodifiableMap(peers);
    }
}

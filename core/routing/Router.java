package core.routing;

import core.model.Packet;
import core.p2p.Peer;

public interface Router {
    void onReceive(Packet packet, Peer from);
    void send(Packet packet);
}
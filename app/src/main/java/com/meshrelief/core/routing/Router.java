package com.meshrelief.core.routing;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.p2p.Peer;

public interface Router {
    void onReceive(Packet packet, Peer from);
    void send(Packet packet);
}
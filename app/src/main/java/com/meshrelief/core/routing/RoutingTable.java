package com.meshrelief.core.routing;

import java.util.HashMap;
import java.util.Map;

public class RoutingTable {
    private final Map<String, String> routes = new HashMap<>();

    public void addRoute(String destinationId, String nextHopId) {
        routes.put(destinationId, nextHopId);
    }

    public String getNextHop(String destinationId) {
        return routes.get(destinationId);
    }
}

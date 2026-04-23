package core.model;

public class Packet {
    private final String packetId;      // unique (for SeenCache)
    private final String sourceId;
    private final String destinationId; // null = broadcast
    private int ttl;
    private final long timestamp;
    private final byte[] payload;

    public Packet(String packetId, String sourceId, String destinationId, int ttl, long timestamp, byte[] payload) {
        this.packetId = packetId;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.ttl = ttl;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getPacketId() {
        return packetId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }
}

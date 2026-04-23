package core.p2p;

public class Peer {
    private final String id;
    private final String name;

    public Peer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

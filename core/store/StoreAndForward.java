package core.store;

public class StoreAndForward {
    public void queue(byte[] payload, String destinationId) {
        // TODO: queue message for later forwarding
    }

    public void flush() {
        // TODO: send queued messages when connectivity is restored
    }
}

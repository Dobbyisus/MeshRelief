package core.crypto;

public class MessageSigner {
    public byte[] sign(byte[] payload, String privateKey) {
        // TODO: sign payload with private key
        return new byte[0];
    }

    public boolean verify(byte[] payload, byte[] signature, String publicKey) {
        // TODO: verify signature against public key
        return false;
    }
}

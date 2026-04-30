package com.meshrelief.core.crypto;

public class Identity {
    private final String publicKey;
    private final String privateKey;

    public Identity(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}

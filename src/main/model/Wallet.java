package main.model;

import java.io.Serializable;
import java.security.*;

public class Wallet implements Serializable {
    private final KeyPair keyPair;

    //constructors for generating new key pairs
    public Wallet() throws NoSuchAlgorithmException {
        this(2048, KeyPairGenerator.getInstance("DSA")); //calls the second constructor with a default
    }
    public Wallet(Integer keySize, KeyPairGenerator keyPairGen) {
        keyPairGen.initialize(keySize);
        this.keyPair = keyPairGen.generateKeyPair();
    }

    //constructor for importing keys only
    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.keyPair = new KeyPair(publicKey, privateKey);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}

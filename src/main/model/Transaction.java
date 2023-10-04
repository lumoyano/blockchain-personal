package main.model;

import java.io.Serializable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

public class Transaction implements Serializable {
    private byte[] from;
    private String fromFX;
    private byte[] to;
    private String toFX;
    private Integer value;
    private String timestamp;
    private byte[] signature;
    private String signatureFX;
    private Integer ledgerID;

    //constructor for loading with existing signature
    public Transaction( byte[] from, byte[] to, byte[] signature,
                        String timestamp,Integer value, Integer ledgerID ) {
        Base64.Encoder encoder = Base64.getEncoder();
        this.from = from;
        this.fromFX = encoder.encodeToString(from);
        this.to = to;
        this.toFX = encoder.encodeToString(to);
        this.value =  value;
        this.signature = signature;
        this.signatureFX = encoder.encodeToString(signature);
        this.ledgerID = ledgerID;
        this.timestamp = timestamp;
    }

    public Transaction(Wallet fromWallet, byte[] toAddress, Integer value,
                       Integer ledgerID, Signature signing)
            throws InvalidKeyException, SignatureException {
        Base64.Encoder encoder = Base64.getEncoder();
        this.from = fromWallet.getPublicKey().getEncoded();
        this.fromFX = encoder.encodeToString(fromWallet.getPublicKey().getEncoded);
        this.to = toAddress;
        this.toFX = encoder.encodeToString(toAddress)
        this.value =  value;
        this.ledgerID = ledgerID;
        this.timestamp = LocalDateTime.now().toString();
        signing.initSign(fromWallet.getPrivateKey());
        String sr = this.toString();
        signing.update(sr.getBytes());
        this.signature = signing.sign();
        this.signatureFX = encoder.encodeToString(this.signature);
    }

    public boolean isVerified(Signature signing) throws InvalidKeyException, SignatureException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(this.getFrom());
        signing.initVerify(keyFactory.generatePublic(keySpec));
        signing.update(this.toString().getBytes());
        return signing.verify(this.signature);
    }

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public String getFromFX() {
        return fromFX;
    }

    public void setFromFX(String fromFX) {
        this.fromFX = fromFX;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public String getToFX() {
        return toFX;
    }

    public void setToFX(String toFX) {
        this.toFX = toFX;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getSignatureFX() {
        return signatureFX;
    }

    public void setSignatureFX(String signatureFX) {
        this.signatureFX = signatureFX;
    }

    public Integer getLedgerID() {
        return ledgerID;
    }

    public void setLedgerID(Integer ledgerID) {
        this.ledgerID = ledgerID;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + Arrays.toString(from) +
                ", fromFX='" + fromFX + '\'' +
                ", to=" + Arrays.toString(to) +
                ", toFX='" + toFX + '\'' +
                ", value=" + value +
                ", timestamp='" + timestamp + '\'' +
                ", signature=" + Arrays.toString(signature) +
                ", signatureFX='" + signatureFX + '\'' +
                ", ledgerID=" + ledgerID +
                '}';
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSignature());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Transaction)) return false;
        Transaction that = (Transaction) obj;
        return Arrays.equals(getSignature(), that.getSignature());
    }
}

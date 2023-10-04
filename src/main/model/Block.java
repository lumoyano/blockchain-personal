package main.model;

import java.io.Serializable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Block implements Serializable {

    private byte[] prevHash;
    private byte[] currHash;
    private byte[] minedBy;
    private Integer ledgerId = 1;
    private String timeStamp;
    private Integer miningPoints = 0;
    private Double luck = 0.0;

    private ArrayList<Transaction> transactionLedger = new ArrayList<Transaction>();

    //constructors
    //constructor to create the initial block
    public Block() {
        prevHash = new byte[]{0};
    }

    //constructor for retrieval from the db
    public Block(byte[] prevHash, byte[] currHash, byte[] minedBy,
                 String timeStamp, Integer ledgerId, Integer miningPoints, Double luck
            , ArrayList<Transaction> transactionLedger
    ) {
        this.prevHash = prevHash;
        this.currHash = currHash;
        this.timeStamp = timeStamp;
        this.minedBy = minedBy;
        this.ledgerId = ledgerId;
        this.transactionLedger = transactionLedger;
        this.miningPoints = miningPoints;
        this.luck = luck;
    }

    //constructor for initializing after retrieval/adding new blocks
    public Block(LinkedList<Block> currentBlockchain) {
        Block lastBlock = currentBlockchain.getLast();
        prevHash = lastBlock.getCurrHash();
        ledgerId = lastBlock.getLedgerId() + 1;
        luck = Math.random() * 1000000;
    }

    public boolean isVerified(
            Signature signing
    ) throws InvalidKeyException, SignatureException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(this.minedBy);
        signing.initVerify(keyFactory.generatePublic(keySpec));
        signing.update(this.toString().getBytes());
        return signing.verify(this.currHash);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block = (Block) o;
        return Arrays.equals(getPrevHash(), block.getPrevHash());
    }

    public int hashCode() {
        return Arrays.hashCode(getPrevHash());
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public byte[] getCurrHash() {
        return currHash;
    }

    public void setPrevHash(byte[] prevHash) {
        this.prevHash = prevHash;
    }

    public void setCurrHash(byte[] currHash) {
        this.currHash = currHash;
    }

    public ArrayList<Transaction> getTransactionLedger() {
        return transactionLedger;
    }

    public void setTransactionLedger(ArrayList<Transaction> transactionLedger) {
        this.transactionLedger = transactionLedger;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getMinedBy() {
        return minedBy;
    }

    public void setMinedBy(byte[] minedBy) {
        this.minedBy = minedBy;
    }

    public Integer getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(Integer miningPoints) {
        this.miningPoints = miningPoints;
    }

    public Double getLuck() {
        return luck;
    }

    public void setLuck(Double luck) {
        this.luck = luck;
    }

    public Integer getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Integer ledgerId) {
        this.ledgerId = ledgerId;
    }

    @Override
    public String toString() {
        return "Block{" +
                "prevHash=" + Arrays.toString(prevHash) +
                ", currHash=" + Arrays.toString(currHash) +
                ", minedBy=" + Arrays.toString(minedBy) +
                ", ledgerId=" + ledgerId +
                ", timeStamp='" + timeStamp + '\'' +
                ", miningPoints=" + miningPoints +
                ", luck=" + luck +
                ", transactionLedger=" + transactionLedger +
                '}';
    }
}


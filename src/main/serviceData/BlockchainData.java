package main.serviceData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import main.model.*;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class BlockchainData {
    private final ObservableList<Transaction> newBlockTransactionFX;
    private final ObservableList<Transaction> newBlockTransactions;
    private LinkedList<Block> currentBlockChain = new LinkedList<>();
    private Block latestBlock;
    private boolean exit = false;
    private int miningPoints;
    private static final int TIMEOUT_INTERVAL = 65;
    private static final int MINING_INTERVAL = 60;
    //helper class
    private final Signature signing = Signature.getInstance("SHA256withDSA");

    //singleton class
    private static BlockchainData instance;

    static {
        try {
            instance = new BlockchainData();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public BlockchainData() throws NoSuchAlgorithmException {
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionFX = FXCollections.observableArrayList();
    }

    public static BlockchainData getInstance() {
        return instance;
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimestamp);

    public ObservableList<Transaction> getTransactionLedgerFX() {
        newBlockTransactionFX.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionFX);
    }

    public String getWalletBalanceFX() {
        return getBalance(currentBlockChain, newBlockTransactions, WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    private Object getBalance(LinkedList<Block> blockChain, ObservableList<Transaction> currentLedger, PublicKey walletAddress) {
        Integer balance = 0;
        for (Block block : blockChain) {
            for (Transaction transaction : block.getTransactionLedger()) {
                if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) balance -= transaction.getValue();
                if (Arrays.equals(transaction.getTo(), walletAddress.getEncoded())) balance += transaction.getValue();
            }
        }
        for (Transaction transaction :
                currentLedger) {
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) balance -= transaction.getValue();
        }
        return balance;
    }

    private void verifyBlockChain(LinkedList<Block> currentBlockChain) throws GeneralSecurityException {
        for (Block block :
                currentBlockChain) {
            if (!block.isVerified(signing)) throw new GeneralSecurityException("Block validation failed");
            ArrayList<Transaction> transactions = block.getTransactionLedger();
            for (Transaction transaction :
                    transactions) {
                if (!transaction.isVerified(signing))
                    throw new GeneralSecurityException("Transaction validation failed");
            }
        }
    }

    public void addTransactionState(Transaction transaction) {
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }

    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException {
        try {
            if (getBalance(currentBlockChain, newBlockTransactions, new DSAPublicKeyImpl(transaction.getFrom()))
                    < transaction.getValue()
                    && !blockReward)
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");
            else {
                Connection connection = DriverManager.getConnection
                        ("jdbc:sqlite:C:\\Users\\lumoy\\OneDrive\\Área de Trabalho\\Workspaces\\blockchain-personal\\db\\blockchain.db");

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getLedgerID());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimestamp());
                pstmt.executeUpdate();
                pstmt.close();
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerID) throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:C:\\Users\\lumoy\\OneDrive\\Área de Trabalho\\Workspaces\\blockchain-personal\\db\\blockchain.db");
            PreparedStatement pstmt = connection.prepareStatement(" SELECT * FROM TRANSACTIONS WHERE " +
                    "LEDGER_ID = ?");
            pstmt.setInt(1, ledgerID);
            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getInt("VALUE"),
                        resultSet.getInt("LEDGER_ID")
                ));
            }
            resultSet.close();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public void loadBlockChain() {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:C:\\Users\\lumoy\\OneDrive\\Área de Trabalho\\Workspaces\\blockchain-personal\\db\\blockchain.db");
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                this.currentBlockChain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }
            latestBlock = currentBlockChain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100,
                    latestBlock.getLedgerId() + 1,
                    signing);
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockChain(currentBlockChain);
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public void mineBlock() {
        try {
            finalizeBlock(WalletData.getInstance().getWallet());
            addBlock(latestBlock);
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
        }
    }

    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException {
        latestBlock = new Block(BlockchainData.getInstance().currentBlockChain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signing.sign());
        currentBlockChain.add(latestBlock);
        miningPoints = 0;
        //reward transaction
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);
        Transaction transaction = new Transaction(new Wallet(),
                minersWallet.getPublicKey().getEncoded(),
                100,
                latestBlock.getLedgerId() + 1,
                signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }

    private void addBlock(Block block) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\lumoy\\OneDrive\\Área de Trabalho\\Workspaces\\blockchain-personal\\db\\blockchain.db");
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, " +
                    "CURRENT_HASH, " +
                    "LEDGER_ID, " +
                    "CREATED_ON, " +
                    "CREATED_BY, " +
                    "MINING_POINTS, " +
                    "LUCK) VALUES(?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrHash());
            pstmt.setInt(3, block.getLedgerId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connetion = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\lumoy\\OneDrive\\Área de Trabalho\\Workspaces\\blockchain-personal\\db\\blockchain.db");
            Statement clearDBStatement = connetion.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connetion.close();
            for (Block block : receivedBC) {
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);
                for (Transaction transaction :
                        block.getTransactionLedger()) {
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC) {
        try {
            //verify the validity of the received blockchain
            verifyBlockChain(receivedBC);
            //check if we have received an identical blockchain
            if (!Arrays.equals(receivedBC.getLast().getCurrHash(), currentBlockChain.getLast().getCurrHash())) {
                if (checkIfOutdated(receivedBC) != null) return getCurrentBlockChain();
                else {
                    if (checkWhichIsCreatedFirst(receivedBC) != null) return getCurrentBlockChain();
                    else if (compareMiningPointsAndLuck(receivedBC) != null) return getCurrentBlockChain();
                }
            } else if (!receivedBC.getLast().getTransactionLedger().equals(getCurrentBlockChain().getLast().getTransactionLedger())) {
                updateTransactionLedgers(receivedBC);
                System.out.println("Transaction Ledgers up`dated");
                return receivedBC;
            } else
                System.out.println("blockchains are identical");
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return receivedBC;
    }

    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBC) {
        //check how old the blockchains are
        long lastMinedLocalBlock = LocalDateTime.parse(getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedRcvdBlock = LocalDateTime.parse(receivedBC.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
            //if both are old do nothing
            System.out.println("both are old, check other peers");
        else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            //if your blockchain is old but the received one is new, use the new
            //reset the mining points since we weren't contributing until now
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            System.out.println("local Blockchain outdated, replaced with received Blockchain");
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            //if received one is old but local one is new, send ours to them
            return getCurrentBlockChain();
        }
        return null;
    }

    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBC) {
        //compare timestamps to see which is created first
        long initRcvBlockTime = LocalDateTime.parse(receivedBC.getFirst().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTime = LocalDateTime.parse(getCurrentBlockChain().getFirst().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if (initRcvBlockTime < initLocalBlockTime) {
            //reset the mining points because blockchain was outdated
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            System.out.println("Peer client Blockchain won, server's bc was old");
        } else if (initLocalBlockTime < initRcvBlockTime) {
            return getCurrentBlockChain();
        }
        return null;
    }

    private LinkedList<Block> compareMiningPointsAndLuck(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        //check if both blockchains have the same prevhashes to confirm they're both contending to mine the last block
        //if the same, compare mining points and then luck to assign the block to who's mining it
        if (receivedBC.equals(getCurrentBlockChain())) {
            if (receivedBC.getLast().getMiningPoints() > getCurrentBlockChain().getLast().getMiningPoints() ||
                    receivedBC.getLast().getMiningPoints().equals(getCurrentBlockChain().getLast().getMiningPoints()) &&
                            receivedBC.getLast().getLuck() > getCurrentBlockChain().getLast().getLuck()) {
                //remove reward and transfer to the winning block
                getCurrentBlockChain().getLast().getTransactionLedger().remove(0);
                for (Transaction transaction :
                        getCurrentBlockChain().getLast().getTransactionLedger()) {
                    if (!receivedBC.getLast().getTransactionLedger().contains(transaction))
                        receivedBC.getLast().getTransactionLedger().add(transaction);
                }
                receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
                //we are returning the mining points since our local block lost
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() + getCurrentBlockChain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBC);
                setCurrentBlockChain(new LinkedList<>());
                loadBlockChain();
                System.out.println("received blockchain won");
            } else {
                //remove the reward transaction from their losing block and transfer to our winning block
                receivedBC.getLast().getTransactionLedger().remove(0);
                for (Transaction transaction :
                        receivedBC.getLast().getTransactionLedger()) {
                    if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                        getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
                return getCurrentBlockChain();
            }
        }
        return null;
    }

    private void updateTransactionLedgers(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        for (Transaction transaction :
                receivedBC.getLast().getTransactionLedger()) {
            if (!getCurrentBlockChain().getLast().getTransactionLedger().contains(transaction)) {
                getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                System.out.println("current ledger id = " + getCurrentBlockChain().getLast().getLedgerId() +
                        " transaction id = " + transaction.getLedgerID());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
        for (Transaction transaction :
                getCurrentBlockChain().getLast().getTransactionLedger()) {
            if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                receivedBC.getLast().getTransactionLedger().add(transaction);
            }
        }
        receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
    }

    public LinkedList<Block> getCurrentBlockChain() {
        return currentBlockChain;
    }

    public void setCurrentBlockChain(LinkedList<Block> currentBlockChain) {
        this.currentBlockChain = currentBlockChain;
    }

    public static int getTimeoutInterval() {
        return TIMEOUT_INTERVAL;
    }

    public static int getMiningInterval() {
        return MINING_INTERVAL;
    }

    public int getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(int miningPoints) {
        this.miningPoints = miningPoints;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }
}

package main.threads;

import main.model.Block;
import main.serviceData.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class PeerRequestThread extends Thread {

    private final Socket socket;

    public PeerRequestThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
            LinkedList<Block> receivedBC = (LinkedList<Block>) objectInput.readObject();
            System.out.println("LedgerID: " + receivedBC.getLast().getLedgerId() +
                    " Size: " + receivedBC.getLast().getTransactionLedger().size());
            objectOutput.writeObject(BlockchainData.getInstance().getBlockchainConsensus(receivedBC));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

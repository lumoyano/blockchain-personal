package main.threads;

import main.model.Block;
import main.serviceData.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerClient extends Thread {
    private Queue<Integer> queue = new ConcurrentLinkedQueue<>();
    public PeerClient() {
        this.queue.add(6001);
        this.queue.add(6002);
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket("127.0.0.1", queue.peek())){
                System.out.println("Sending Blockchain object on port: " + queue.peek());
                queue.add(queue.poll());
                socket.setSoTimeout(5000);

                ObjectOutputStream objectOutPut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

                LinkedList<Block> blockchain = BlockchainData.getInstance().getCurrentBlockChain();
                objectOutPut.writeObject(blockchain);

                LinkedList<Block> returnedBlockchain = (LinkedList<Block>) objectInput.readObject();
                System.out.println(" RETURNED BC LedgerID = " + returnedBlockchain.getLast().getLedgerID() +
                        " Size = " + returnedBlockchain.getLast().getTransactionLedger().size());
                BlockchainData.getInstance().getBlockchainConsensus(returnedBlockchain);
                Thread.sleep(2000);
            } catch (SocketTimeoutException e) {
                System.out.println("The socket timed out");
                queue.add(queue.poll());
            } catch (IOException e) {
                System.out.println("Client Error: " +
                        e.getMessage() + " -- Error on port: " + queue.peek());
                queue.add(queue.poll());
            } catch (InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
                queue.add(queue.poll());
            }
        }
    }
}

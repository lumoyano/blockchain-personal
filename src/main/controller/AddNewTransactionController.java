package main.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import main.model.Transaction;
import main.serviceData.BlockchainData;
import main.serviceData.WalletData;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;

public class AddNewTransactionController {

    @FXML
    private TextField toAddress;
    @FXML
    private TextField value;

    @FXML
    public void createNewTransaction () throws GeneralSecurityException {
        Base64.Decoder Decoder = Base64.getDecoder();
        Signature signing = Signature.getInstance("SHA256withDSA");
        Integer ledgerID = BlockchainData.getInstance().getTransactionLedgerFX().get(0).getLedgerID();
        byte[] sendB = Decoder.decode(toAddress.getText());
        Transaction transaction = new Transaction(WalletData.getInstance().getWallet(), sendB,
                Integer.parseInt(value.getText()), ledgerID, signing);
        BlockchainData.getInstance().addTransaction(transaction, false);
        BlockchainData.getInstance().addTransactionState(transaction);
    }

}

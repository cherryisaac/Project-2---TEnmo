package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import com.techelevator.tenmo.model.ViewTransferDTO;

import java.util.List;

public interface UserDao {

    List<User> findAll();

    User findByUsername(String username);

    int findIdByUsername(String username);

    boolean create(String username, String password);

    Double viewBalance(String username);

    Double sendBucks(String senderUserId, Double amount, int recipientUserId);

    List<Transfer> viewTransfer(String username);
}

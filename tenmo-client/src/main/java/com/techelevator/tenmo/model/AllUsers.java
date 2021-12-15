package com.techelevator.tenmo.model;

import java.util.ArrayList;
import java.util.List;

public class AllUsers {
    private List<User> users;

    public AllUsers(){
        users = new ArrayList<>();
    }
    public AllUsers(List<User> users){
        this.users = users;
    }

    public List<User> getAllUsers() {
        return users;
    }

    public void setAllUsers(List<User> allUsers) {
        this.users = allUsers;
    }
}

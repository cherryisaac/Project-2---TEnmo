package com.techelevator.tenmo.controller;

import javax.validation.Valid;

import com.techelevator.tenmo.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.security.jwt.TokenProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to authenticate users.
 */
@RestController
public class AuthenticationController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private UserDao userDao;

    public AuthenticationController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder, UserDao userDao) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDao = userDao;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public LoginResponse login(@Valid @RequestBody LoginDTO loginDto) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.createToken(authentication, false);
        
        User user = userDao.findByUsername(loginDto.getUsername());

        return new LoginResponse(jwt, user);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public void register(@Valid @RequestBody RegisterUserDTO newUser) {
        if (!userDao.create(newUser.getUsername(), newUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User registration failed.");
        }
    }

    @RequestMapping(value = "/viewBalance", method = RequestMethod.POST)
    public ViewBalanceResponse viewBalance(@Valid @RequestBody TokenDTO token){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)tokenProvider.getAuthentication(token.getToken());
        Double balance = userDao.viewBalance(auth.getName());
        return new ViewBalanceResponse(balance);
    }

    @RequestMapping(value = "/sendBucks", method = RequestMethod.POST)
    public sendBucksResponse sendBucks(@Valid @RequestBody TransferDTO transfer){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)tokenProvider.getAuthentication(transfer.getToken());
        Double balance = userDao.sendBucks(auth.getName(), transfer.getAmount(), transfer.getRecipientId());
        return new sendBucksResponse(balance);
    }

    @RequestMapping(value = "/findAllUsers", method = RequestMethod.GET)
    public findAllUsersResponse findAllUsers(){
        List<SimpleUser> users = new ArrayList<>();
        for (User user : userDao.findAll()){
            SimpleUser simpleUser = new SimpleUser();
            simpleUser.setUsername(user.getUsername());
            simpleUser.setId((int)user.getId().longValue());
            users.add(simpleUser);
            }
        return new findAllUsersResponse(users);
    }

    @RequestMapping(value = "/viewPastTransfers", method = RequestMethod.POST)
    public viewPastTransfersResponse viewPastTransfers(@Valid @RequestBody ViewTransferDTO transferDTO){
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)tokenProvider.getAuthentication(transferDTO.getToken());
        List<Transfer> transfer = userDao.viewTransfer(auth.getName());
        viewPastTransfersResponse view = new viewPastTransfersResponse(transfer);
        return view;
    }


    /**
     * Object to return as body in JWT Authentication.
     */
    static class LoginResponse {

        private String token;
        private User user;

        LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        void setToken(String token) {
            this.token = token;
        }

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}
    }

    static class ViewBalanceResponse {

        private Double balance;

        ViewBalanceResponse(Double balance){
            this.balance = balance;
        }

        public Double getBalance() {
            return balance;
        }

        public void setBalance(Double balance) {
            this.balance = balance;
        }
    }

    static class sendBucksResponse {
        private Double balance;

        sendBucksResponse(Double balance){
            this.balance = balance;
        }

        public Double getBalance() {
            return balance;
        }

        public void setBalance(Double balance) {
            this.balance = balance;
        }
    }

    static class findAllUsersResponse{
        private List<SimpleUser> users;

        findAllUsersResponse(List<SimpleUser> users){
            this.users = users;
        }

        public List<SimpleUser> getUsers() {
            return users;
        }

        public void setUsers(List<SimpleUser> users) {
            this.users = users;
        }
    }

    static class viewPastTransfersResponse{
        private List<Transfer> transfers;

        viewPastTransfersResponse(List<Transfer> transfers){
            this.transfers = transfers;
        }

        public List<Transfer> getTransfers() {
            return transfers;
        }

        public void setTransfers(List<Transfer> transfers) {
            this.transfers = transfers;
        }
    }
}
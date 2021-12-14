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
        return new findAllUsersResponse(userDao.findAll());
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
        private Double amount;

        sendBucksResponse(Double amount){
            this.amount = amount;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }

    static class findAllUsersResponse{
        private List<User> users;

        findAllUsersResponse(List<User> users){
            this.users = users;
        }

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }
}


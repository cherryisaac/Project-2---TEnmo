package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import com.techelevator.tenmo.model.ViewTransferDTO;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcUserDao implements UserDao {

    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");
    private JdbcTemplate jdbcTemplate;

    public JdbcUserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int findIdByUsername(String username) {
        String sql = "SELECT user_id FROM users WHERE username ILIKE ?;";
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class, username);
        if (id != null) {
            return id;
        } else {
            return -1;
    }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash FROM users;";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while(results.next()) {
            User user = mapRowToUser(results);
            users.add(user);
        }
        return users;
    }

    @Override
    public User findByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT user_id, username, password_hash FROM users WHERE username ILIKE ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()){
            return mapRowToUser(rowSet);
            }
        throw new UsernameNotFoundException("User " + username + " was not found.");
    }

    @Override
    public boolean create(String username, String password) {

        // create user
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING user_id";
        String password_hash = new BCryptPasswordEncoder().encode(password);
        Integer newUserId;
        try {
            newUserId = jdbcTemplate.queryForObject(sql, Integer.class, username, password_hash);
        } catch (DataAccessException e) {
            return false;
                }

        // create account
        sql = "INSERT INTO accounts (user_id, balance) values(?, ?)";
        try {
            jdbcTemplate.update(sql, newUserId, STARTING_BALANCE);
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    @Override
    public Double viewBalance(String username) {
        String sql = "select accounts.balance from accounts " +
                "join users on accounts.user_id = users.user_id " +
                "where users.username = ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()) {
            return rowSet.getDouble(1);
        }
        else throw new UsernameNotFoundException("Can not get balance for " + username);
    }

    @Override
    public Double sendBucks(String senderUserId, Double amount, int recipientUserId) {

        int senderAccountId = getAccountId(senderUserId);
        int recipientAccountId = getAccountByUserId(recipientUserId);
        String sqlStarting = "SELECT balance FROM accounts WHERE account_id = ?";
        SqlRowSet rowset = jdbcTemplate.queryForRowSet(sqlStarting, senderAccountId);
        Double startingBalance = 0.0;
        if (rowset.next()){
            startingBalance = rowset.getDouble(1);
        }
        else throw new NullPointerException("Not enough bucks, can not send bucks to yourself, or recipient ID may be invalid.");

        if (startingBalance >= amount && senderAccountId != recipientAccountId ) {
            String sql = "insert into transfers (transfer_type_id, transfer_status_id, account_from, account_to, amount) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 2, 2, senderAccountId, recipientAccountId, amount);
            String sqlUpdate = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            jdbcTemplate.update(sqlUpdate, amount, senderAccountId);
            String sqlUpdate2 = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            jdbcTemplate.update(sqlUpdate2, amount, recipientAccountId);
            String sqlRemaining = "SELECT balance FROM accounts WHERE account_id = ?";
            SqlRowSet rowset2 = jdbcTemplate.queryForRowSet(sqlRemaining, senderAccountId);
            if (rowset2.next()) {
                return rowset2.getDouble(1);
            } else throw new NullPointerException("Not enough bucks, can not send bucks to yourself, or recipient ID may be invalid.");
        }
        else throw new NullPointerException("Not enough bucks, can not send bucks to yourself, or recipient ID may be invalid.");
    }

    private int getAccountId(String username){
        String sql = "select accounts.account_id from accounts " +
                "join users on accounts.user_id = users.user_id " +
                "where users.username = ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()) {
            return rowSet.getInt(1);
        }
        else throw new UsernameNotFoundException("Can not get account_id for " + username);
    }

    private int getAccountByUserId(int userId){
        String sql = "select accounts.account_id from accounts " +
                "where accounts.user_id = ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, userId);
        if (rowSet.next()) {
            return rowSet.getInt(1);
        }
        else throw new UsernameNotFoundException("Can not get account_id for " + userId);
    }

    public List<Transfer> viewTransfer(String username){
        int accountId = getAccountId(username);
        String sql = "select transfer_types.transfer_type_desc, transfer_statuses.transfer_status_desc, transfers.transfer_id, transfers.account_from, transfers.account_to, transfers.amount " +
        "from transfers " +
        "join transfer_types on transfers.transfer_type_id = transfer_types.transfer_type_id " +
        "join transfer_statuses on transfers.transfer_status_id = transfer_statuses.transfer_status_id " +
                "where account_from = ? or account_to = ?";

        SqlRowSet rowset = jdbcTemplate.queryForRowSet(sql, accountId, accountId);
        List<Transfer> transfer = new ArrayList<>();
        while (rowset.next()){
            Transfer transfer1 = new Transfer();
            transfer1.setTransfer_type(rowset.getString(1));
            transfer1.setTransfer_status(rowset.getString(2));
            transfer1.setTransfer_id(rowset.getInt(3));
            transfer1.setAccount_from(rowset.getInt(4));
            transfer1.setAccount_to(rowset.getInt(5));
            transfer1.setAmount(rowset.getDouble(6));
            transfer.add(transfer1);
        }
        return transfer;
    }

    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setActivated(true);
        user.setAuthorities("USER");
        return user;
    }
}

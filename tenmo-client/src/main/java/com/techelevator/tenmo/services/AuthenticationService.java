package com.techelevator.tenmo.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.techelevator.tenmo.model.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class AuthenticationService {

    private String baseUrl;
    private RestTemplate restTemplate = new RestTemplate();

    public AuthenticationService(String url) {
        this.baseUrl = url;
    }

    public AuthenticatedUser login(UserCredentials credentials) throws AuthenticationServiceException {
        HttpEntity<UserCredentials> entity = createRequestEntity(credentials);
        return sendLoginRequest(entity);
    }

    public void register(UserCredentials credentials) throws AuthenticationServiceException {
    	HttpEntity<UserCredentials> entity = createRequestEntity(credentials);
        sendRegistrationRequest(entity);
    }
    
	private HttpEntity<UserCredentials> createRequestEntity(UserCredentials credentials) {
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	HttpEntity<UserCredentials> entity = new HttpEntity<>(credentials, headers);
    	return entity;
    }

	private AuthenticatedUser sendLoginRequest(HttpEntity<UserCredentials> entity) throws AuthenticationServiceException {
		try {	
			ResponseEntity<AuthenticatedUser> response = restTemplate.exchange(baseUrl + "login", HttpMethod.POST, entity, AuthenticatedUser.class);
			return response.getBody(); 
		} catch(RestClientResponseException ex) {
			String message = createLoginExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
        }
	}

    private ResponseEntity<Map> sendRegistrationRequest(HttpEntity<UserCredentials> entity) throws AuthenticationServiceException {
    	try {
			return restTemplate.exchange(baseUrl + "register", HttpMethod.POST, entity, Map.class);
		} catch(RestClientResponseException ex) {
			String message = createRegisterExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
        }
	}

	private String createLoginExceptionMessage(RestClientResponseException ex) {
		String message = null;
		if (ex.getRawStatusCode() == 401 && ex.getResponseBodyAsString().length() == 0) {
		    message = ex.getRawStatusCode() + " : {\"timestamp\":\"" + LocalDateTime.now() + "+00:00\",\"status\":401,\"error\":\"Invalid credentials\",\"message\":\"Login failed: Invalid username or password\",\"path\":\"/login\"}";
		}
		else {
		    message = ex.getRawStatusCode() + " : " + ex.getResponseBodyAsString();
		}
		return message;
	}
	
	private String createRegisterExceptionMessage(RestClientResponseException ex) {
		String message = null;
		if (ex.getRawStatusCode() == 400 && ex.getResponseBodyAsString().length() == 0) {
		    message = ex.getRawStatusCode() + " : {\"timestamp\":\"" + LocalDateTime.now() + "+00:00\",\"status\":400,\"error\":\"Invalid credentials\",\"message\":\"Registration failed: Invalid username or password\",\"path\":\"/register\"}";
		}
		else {
		    message = ex.getRawStatusCode() + " : " + ex.getResponseBodyAsString();
		}
		return message;
	}

	private HttpEntity<Token> createRequestEntity(Token token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Token> entity = new HttpEntity<>(token, headers);
		return entity;
	}

	private Balance sendViewBalanceRequest(HttpEntity<Token> entity) throws AuthenticationServiceException {
		try {
			ResponseEntity<Balance> response = restTemplate.exchange(baseUrl + "viewBalance", HttpMethod.POST, entity, Balance.class);
			return response.getBody();
		} catch(RestClientResponseException ex) {
			String message = createLoginExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
		}
	}

	public Double viewBalance(String token) throws AuthenticationServiceException {
		Token tokenObj = new Token();
		tokenObj.setToken(token);
		HttpEntity<Token> entity = createRequestEntity(tokenObj);
		return sendViewBalanceRequest(entity).getBalance();
	}

	public List<User> findAllUsers() throws AuthenticationServiceException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<User> entity = new HttpEntity<>(headers);
			ResponseEntity<AllUsers> response = restTemplate.exchange(baseUrl + "findAllUsers", HttpMethod.GET, entity, AllUsers.class);
			return response.getBody().getAllUsers();
		} catch(RestClientResponseException ex) {
			String message = createLoginExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
		}
	}

	public Double sendBucks(String token, int recipientId, Double amount) throws AuthenticationServiceException{
		Transfer transfer = new Transfer(token, recipientId, amount);
		try {
			ResponseEntity<Balance> response = restTemplate.exchange(baseUrl + "sendBucks", HttpMethod.POST, createRequestEntity(transfer), Balance.class);
			return response.getBody().getBalance();
		} catch(RestClientResponseException ex) {
			String message = createLoginExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
		}
	}

	private HttpEntity<Transfer> createRequestEntity(Transfer transfer) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Transfer> entity = new HttpEntity<>(transfer, headers);
		return entity;
	}

	public List<TransferDetail> transfers(String token) throws AuthenticationServiceException{
		Token token1 = new Token();
		token1.setToken(token);
		try {
			ResponseEntity<TransferHistory> response = restTemplate.exchange(baseUrl + "viewPastTransfers", HttpMethod.POST, createRequestEntity(token1), TransferHistory.class);
			return response.getBody().getTransfers();
		} catch(RestClientResponseException ex) {
			String message = createLoginExceptionMessage(ex);
			throw new AuthenticationServiceException(message);
		}
	}

	private HttpEntity<Void> makeAuthEntity() {
		AuthenticatedUser currentUser = new AuthenticatedUser();
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(currentUser.getToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(headers);
	}

}

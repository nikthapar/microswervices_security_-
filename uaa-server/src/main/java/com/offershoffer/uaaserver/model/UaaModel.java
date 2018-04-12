package com.offershoffer.uaaserver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "UaaModel")
public class UaaModel {
	
	@Id
	private String userId;
	private String token ; 
	private String publickey;
	private int activeUser;
	
	
	public int  getActiveUser() {
		return activeUser;
	}

	public void setActiveUser(int  activeUser) {
		this.activeUser = activeUser;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getPublickey() {
		return publickey;
	}

	public void setPublickey(String publickey) {
		this.publickey = publickey;
	}

	public UaaModel(String userId, String token, String publickey,int  activeUser) {
		super();
		this.userId = userId;
		this.token = token;
		this.publickey = publickey;
		this.activeUser=activeUser;
	}

}

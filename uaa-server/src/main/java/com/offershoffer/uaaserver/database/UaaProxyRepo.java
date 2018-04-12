package com.offershoffer.uaaserver.database;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.offershoffer.uaaserver.model.UaaModel;

public interface UaaProxyRepo extends MongoRepository<UaaModel, String> {

	public List<UaaModel> findByToken(String token);

}

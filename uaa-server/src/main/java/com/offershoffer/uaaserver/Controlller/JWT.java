package com.offershoffer.uaaserver.Controlller;

import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.offershoffer.uaaserver.database.UaaProxyRepo;
import com.offershoffer.uaaserver.feignproxy.LoginProxyRepo;
import com.offershoffer.uaaserver.feignproxy.RegisterProxyRepo;
import com.offershoffer.uaaserver.model.LoginInfo;
import com.offershoffer.uaaserver.model.RegisterInfo;
import com.offershoffer.uaaserver.model.UaaModel;

@RestController
public class JWT {

	@Autowired
	private LoginProxyRepo loginproxyrepo;

	@Autowired
	private RegisterProxyRepo registerproxyrepo;

	@Autowired
	private UaaProxyRepo uaaproxyrepo;

	@PostMapping("/token/login")
	public String tokenlogin(@RequestBody LoginInfo obj) {
		// check the user credensial are true
		String retrieveinfo = loginproxyrepo.verifyUser(obj);
		// if user credensial are not same return false
		if (retrieveinfo.equals("Unauthorized"))
			return "Unauthorized";

		String[] data = retrieveinfo.split(",");
		String uname = data[0];
		String role = data[1];
		// call function for make token
		String token = createJwtToken(uname, role);
		String[] tokeninfo = token.split(",");
		String jwt = tokeninfo[0];
		String publickey = tokeninfo[1];
		// check if token already exits
		Optional<UaaModel> uaaobject = uaaproxyrepo.findById(uname);
		int activeUser = uaaobject.get().getActiveUser();
		// if uaaobject is not present add entry to database
		if (!uaaobject.isPresent()) {
			token = jwt;
			uaaproxyrepo.save(new UaaModel(uname, jwt, publickey, activeUser + 1));
			return token;
		}
		// else get saved token from database
		uaaproxyrepo.save(new UaaModel(uaaobject.get().getUserId(), uaaobject.get().getToken(),
				uaaobject.get().getPublickey(), activeUser + 1));
		return uaaobject.get().getToken();

	}

	@PostMapping("/token/register")
	public String tokenregister(@RequestBody RegisterInfo obj) {
		String retriveinfo = registerproxyrepo.newUser(obj);
		// if user already exists return registered
		if (retriveinfo.equals("Alredy Exit User"))
			return "Already Register";
		// else get user name and role
		String[] data = retriveinfo.split(",");
		String uname = data[0];
		String role = data[1];
		// generate token
		String token = createJwtToken(uname, role);
		String[] tokeninfo = token.split(",");
		// get token and public key
		String jwt = tokeninfo[0];
		String publickey = tokeninfo[1];
		// save the public key and token along with user count in db
		uaaproxyrepo.save(new UaaModel(uname, jwt, publickey, 1));
		return jwt;
	}

	public String createJwtToken(String uname, String role) {

		RsaJsonWebKey rsaJsonWebKey = null;

		try {
			rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
		} catch (JoseException e1) {
			return "UnAuthorized";
		}

		// Give the JWK a Key ID (kid), which is just the polite thing to do
		rsaJsonWebKey.setKeyId("k1");

		// Create the Claims, which will be the content of the JWT
		JwtClaims claims = null;

		// Create the Claims, which will be the content of the JWT
		claims = new JwtClaims();
		claims.setIssuer("Issuer"); // who creates the token and signs it
		claims.setAudience("Audience"); // to whom the token is intended to be sent
		claims.setExpirationTimeMinutesInTheFuture(10); // time when the token will expire (10 minutes from now)
		claims.setGeneratedJwtId(); // a unique identifier for the token
		claims.setIssuedAtToNow(); // when the token was issued/created (now)
		claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
		claims.setSubject(role); // the subject/principal is whom the token is about
		claims.setClaim("username", uname); // additional claims/attributes about the subject can be added

		// A JWT is a JWS and/or a JWE with JSON claims as the payload.
		// In this example it is a JWS so we create a JsonWebSignature object.
		JsonWebSignature jws = new JsonWebSignature();

		// The payload of the JWS is JSON content of the JWT Claims
		jws.setPayload(claims.toJson());

		// The JWT is signed using the private key
		jws.setKey(rsaJsonWebKey.getPrivateKey());

		// Set the Key ID (kid) header because it's just the polite thing to do.
		// We only have one key in this example but a using a Key ID helps
		// facilitate a smooth key rollover process
		jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());

		// Set the signature algorithm on the JWT/JWS that will integrity protect the
		// claims
		jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

		// Sign the JWS and produce the compact serialization or the complete JWT/JWS
		// representation, which is a string consisting of three dot ('.') separated
		// base64url-encoded parts in the form Header.Payload.Signature
		// If you wanted to encrypt it, you can simply set this jwt as the payload
		// of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
		String jwt = null;
		try {
			jwt = jws.getCompactSerialization();
		} catch (JoseException e1) {
			return "UnAuthorized";
		}

		// A JSON string with only the public key info
		String publicKeyJwkString = rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);

		// TODO save token and key to database
		System.out.println(publicKeyJwkString);
		// return token
		return jwt;
	}

	@GetMapping("/verifytoken/{applicationToken}")
	public String verifyTokenComingFromService(@PathVariable String applicationToken) {
		// public key from token db
		List<UaaModel> userFound = uaaproxyrepo.findByToken(applicationToken);
		if (userFound.size() == 0)
			return "UnAuthorized";
		UaaModel uaaModel = userFound.get(0);
		PublicJsonWebKey parsedPublicKeyJwk = null;
		try {
			parsedPublicKeyJwk = PublicJsonWebKey.Factory.newPublicJwk(uaaModel.getPublickey());
		} catch (JoseException e2) {
			return "UnAuthorized";
		}

		// TODO get public key from databse
		PublicKey publicKey = parsedPublicKeyJwk.getPublicKey();

		// Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
		// be used to validate and process the JWT.
		// The specific validation requirements for a JWT are context dependent,
		// however,
		// it typically advisable to require a (reasonable) expiration time, a trusted
		// issuer, and
		// and audience that identifies your system as the intended recipient.
		// If the JWT is encrypted too, you need only provide a decryption key or
		// decryption key resolver to the builder.
		JwtConsumer jwtConsumer = new JwtConsumerBuilder().setRequireExpirationTime() // the JWT must have an expiration
																						// time
				.setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for
													// clock skew
				.setRequireSubject() // the JWT must have a subject claim
				.setExpectedIssuer("Issuer") // whom the JWT needs to have been issued by
				.setExpectedAudience("Audience") // to whom the JWT is intended for
				.setVerificationKey(publicKey) // verify the signature with the public key
				.setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
						new AlgorithmConstraints(ConstraintType.WHITELIST, // which is only RS256 here
								AlgorithmIdentifiers.RSA_USING_SHA256))
				.build(); // create the JwtConsumer instance

		// create the JwtConsumer instance
		try {
			// Validate the JWT and process it to the Claims
			JwtClaims jwtClaims = jwtConsumer.processToClaims(applicationToken);
			System.out.println("JWT validation succeeded! " + jwtClaims);
			try {
				return jwtClaims.getClaimValue("username") + "," + jwtClaims.getSubject();
			} catch (MalformedClaimException e) {
				return "UnAuthorized";
			}
		} catch (InvalidJwtException e) {
			// InvalidJwtException will be thrown, if the JWT failed processing or
			// validation in anyway.
			// Hopefully with meaningful explanations(s) about what went wrong.
			System.out.println("Invalid JWT! " + e);

			// Programmatic access to (some) specific reasons for JWT invalidity is also
			// possible
			// should you want different error handling behavior for certain conditions.

			// Whether or not the JWT has expired being one common reason for invalidity
			if (e.hasExpired()) {
				try {
					System.out.println("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
				} catch (MalformedClaimException e1) {
					return "UnAuthorized";
				}
			}

			// Or maybe the audience was invalid
			if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID)) {
				try {
					System.out.println("JWT had wrong audience: " + e.getJwtContext().getJwtClaims().getAudience());
				} catch (MalformedClaimException e1) {
					return "UnAuthorized";
				}
			}
			try {
				throw new Exception("UnAuthorized");
			} catch (Exception e1) {
				return "UnAuthorized";
			}
		}
	}
}

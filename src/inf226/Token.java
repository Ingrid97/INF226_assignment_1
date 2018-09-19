package inf226;

import java.security.SecureRandom;
import java.util.Base64;

public final class Token {

	// TODO: This should be an immutable class representing a token.

	String encodedUsername;

	/**
	 * The constructor should generate a random 128 bit token
	 */
	public Token(String username){
		// TODO:  generate a random 128 bit token

		 this.encodedUsername = Base64.getEncoder().encodeToString(username.getBytes());
		/*
		SecureRandom random = new SecureRandom();
		byte bytes[] = new byte[16]; // 128 bits are converted to 16 bytes;
		random.nextBytes(bytes);*/
	}
	
	/**
	 * This method should return the Base64 encoding of the token
	 * @return A Base64 encoding of the token
	 */
	public String stringRepresentation() {
		return encodedUsername;
	}
}

package inf226;

import java.io.*;
import java.net.*;
import java.util.function.Function;

import inf226.Storage.Id;
import inf226.Storage.KeyedStorage;
import inf226.Storage.Storage.ObjectDeletedException;
import inf226.Storage.Stored;
import inf226.Storage.TransientStorage;

import static inf226.Maybe.just;

/**
 * 
 * The Server main class. This implements all critical server functions.
 * 
 * @author INF226
 *
 */
public class Server {
	private static final int portNumber = 1337;
	private static final KeyedStorage<String,User> storage
	  = new TransientStorage<String,User>
	         (new Function<User,String>()
	        		 {public String apply(User u)
	        		 {return u.getName();}});
	
	public static Maybe<Stored<User>> authenticate(String username, String password) {
		// TODO: Implement user authentication
		return Maybe.nothing();
	}

	public static Maybe<Stored<User>> register(String username, String password) {
		// TODO: Implement user registration
		User u = new User(username);
		Id.Generator g = new Id.Generator();
		Stored k = new Stored(g, u);
		//return Maybe.nothing();
		return just(k);
	}
	
	public static Maybe<Token> createToken(Stored<User> user) {
		// TODO: Implement token creation
		return Maybe.nothing();
	}
	public static Maybe<Stored<User>> authenticate(String username, Token token) {
		// TODO: Implement user authentication
		return Maybe.nothing();
	}

	public static Maybe<String> validateUsername(String username) {
		// TODO: Validate username before returning
		return just(username);
	}

	public static Maybe<String> validatePassword(String pass) {
		// TODO: Validate pass before returning
		// This method only checks that the password contains a safe string.
		return just(pass);
	}

	public static Maybe<Stored<User>> sendMessage(Stored<User> sender, Message message) {
		// TODO: Implement the message sending.
		return Maybe.nothing();
	}
	
	/**
	 * Refresh the stored user object from the storage.
	 * @param user
	 * @return Refreshed value. Nothing if the object was deleted.
	 */
	public static Maybe<Stored<User>> refresh(Stored<User> user) {
		try {
			return just(storage.refresh(user));
		} catch (ObjectDeletedException e) { 
		} catch (IOException e) { 
		}
		return Maybe.nothing();
	}
	
	/**
	 * @param args TODO: Parse args to get port number
	 */
	public static void main(String[] args) {
		final RequestProcessor processor = new RequestProcessor();
		System.out.println("Staring authentication server");
		processor.start();
		try (final ServerSocket socket = new ServerSocket(portNumber)) {
            while(!socket.isClosed()) {
            	System.err.println("Waiting for client to connect…");
        		Socket client = socket.accept();
            	System.err.println("Client connected.");
        		processor.addRequest(new RequestProcessor.Request(client));
			}
		} catch (IOException e) {
			System.out.println("Could not listen on port " + portNumber);
			e.printStackTrace();
		}
	}


}

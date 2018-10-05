package inf226;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.function.Function;
import inf226.Storage.KeyedStorage;
import inf226.Storage.Storage.ObjectDeletedException;
import inf226.Storage.Stored;
import inf226.Storage.TransientStorage;

import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 * The Server main class. This implements all critical server functions.
 *
 * @author INF226
 *
 */
public class Server {
	private static final int portNumber = 1337;
	private static ArrayList<User> users = new ArrayList<>();
	private static final DataBaseUserStorage storage = DataBaseUserStorage.getInstance();

	// Brukes ved login, usikker på om det er riktig
	public static Maybe<Stored<User>> authenticate(String username, String password) throws Maybe.NothingException {
		try {
			UserName userN = new UserName(username);
			Maybe<Stored<User>> u = storage.lookup(userN);

			try {
				// The user does not get to know if the password or username fails. This is open for discussion
				Password inn = new Password(password, u.force().getValue().getSalt());
				Password pass = new Password(u.force().getValue().getPassword());
				if (!inn.equals(pass) || !username.equals(u.force().getValue().getName())) {
					return Maybe.nothing();
				}
			} catch (Maybe.NothingException n) {
				n.getStackTrace();
			}
			return storage.lookup(userN);
		} catch (Exception e) {
			e.printStackTrace();
			return Maybe.nothing();
		}
	}

	public static Maybe<Stored<User>> register(UserName username, Password password) throws IOException {

		try {
			User u = new User(username.username,password.toString(), password.getSalt());
			return Maybe.just(storage.save(u));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return Maybe.nothing();
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
		// This method only checks that the username contains a safe string.
		if (username.matches("^[a-zA-Z0-9]+")) return Maybe.just(username);

		return Maybe.nothing();
	}

	public static Maybe<String> validatePassword(String pass) {
		if(pass.matches("[\\w\\d.,:;()\\[\\]{}<>\"'#!$%&/+*?=_|\\-]*")) return Maybe.just(pass);

		return Maybe.nothing();
	}

	public static boolean sendMessage(Stored<User> sender, String recipient, Message message, BufferedWriter out) {
		try{
			UserName userN = new UserName(recipient);
			if(!storage.lookup(userN).isNothing()){
				System.out.println("hit: " + storage.lookup(userN).force().getValue().getName());
				Maybe<Stored<User>> user = storage.lookup(userN);
				System.out.println(user.force().getValue().getName());
				User new_user = user.force().getValue().addMessage(message);
				storage.update(user.force(), new_user);
				return true;
			}
			else return false;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Refresh the stored user object from the storage.
	 * @param user
	 * @return Refreshed value. Nothing if the object was deleted.
	 */
	public static Maybe<Stored<User>> refresh(Stored<User> user) {
		try {
			return Maybe.just(storage.refresh(user));
		} catch (ObjectDeletedException e) {
		} catch (IOException e) {
		}
		return Maybe.nothing();
	}

	/**
	 * @param args TODO: Parse args to get port number
	 */
	public static void main(String[] args) {
		System.setProperty("javax.net.ssl.keyStore", "inf226.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "test123");

		final RequestProcessor processor = new RequestProcessor();
		System.out.println("Starting authentication server");
		storage.connect();
		storage.makeTable();
		processor.start();

		SSLServerSocketFactory factory = null;
		try{
			factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		} catch(Exception e){
			System.out.println("Catched exception " + e);
		}

		if (factory != null) {
			try (final ServerSocket socket = factory.createServerSocket(portNumber)) {
                while(!socket.isClosed()) {
                    System.err.println("Waiting for client to connect…");
                    Socket client = socket.accept();
                    System.err.println("Client connected.");
                    processor.addRequest(new RequestProcessor.Request(client));
                }
            } catch (IOException  e) {
                System.out.println("Could not listen on port " + portNumber);
                e.printStackTrace();
            }
		}
	}
}
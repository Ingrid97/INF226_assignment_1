package inf226;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.function.Function;
import inf226.Storage.KeyedStorage;
import inf226.Storage.Storage.ObjectDeletedException;
import inf226.Storage.Stored;
import inf226.Storage.TransientStorage;

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
	private static final KeyedStorage<String,User> storage
			= new TransientStorage<String,User>
			(new Function<User,String>()
			{public String apply(User u)
			{return u.getName();}});

	// Brukes ved login, usikker på om det er riktig
	public static Maybe<Stored<User>> authenticate(String username, String password) {
		Maybe<Stored<User>> u = storage.lookup(username);

		try{
			if (!password.equals(u.force().getValue().getPassword())){
				System.out.println("You entered a wrong password.");
				return Maybe.nothing();
			}

			if (!username.equals(u.force().getValue().getName())){
				System.out.println("Wrong username.");
				return Maybe.nothing();
			}
		}
		catch(Maybe.NothingException n){
			n.getStackTrace();
		}
		return storage.lookup(username);
	}

	public static Maybe<Stored<User>> register(String username, String password) throws IOException {

		try {

			User u = new User(username,password);
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
		// TODO: Validate username before returning
		if (username.matches("^[a-zA-Z0-9]+"))
			return Maybe.just(username);
		return Maybe.nothing();
	}

	public static Maybe<String> validatePassword(String pass) {
		// This method only checks that the password contains a safe string.


		// Aorks but not for the right characters.
		//TODO: add for .,:;()[]{}<>"'#!$%&/+*?=-_|

		if(pass.matches("^[a-zA-Z1-9]+") /*|| pass.matches("")*/)
			return Maybe.just(pass);
		return Maybe.nothing();

	}

	public static boolean sendMessage(Stored<User> sender, String recipient, String content) {
		try{
			Message msg = new Message(sender.getValue(), recipient, content);

			// FIND THE RECIPIENT USER
			//sender.getValue().addMessage(msg);

			return true;
		}
		catch(Message.Invalid e){
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
		final RequestProcessor processor = new RequestProcessor();
		System.out.println("Starting authentication server");
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
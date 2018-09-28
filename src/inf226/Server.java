
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

	// Brukes ved login
	public static Maybe<Stored<User>> authenticate(String username, String password) {
		// TODO: Implement user authentication

		// Check that user is stored?
		//storage.lookup(password);

		//if(storage.lookup(password)){

//		}

		return storage.lookup(username);
	}

	public static Maybe<Stored<User>> register(String username, String password) throws IOException {
		if (validateUsername(username).isNothing()) {
			System.out.println("Nothing");
			return Maybe.nothing();
		}
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


		return Maybe.just(username);
	}

	public static Maybe<String> validatePassword(String pass) {
		// TODO: Validate pass before returning
		// This method only checks that the password contains a safe string.

		System.out.println("Validate Password: " + pass);
		//System.out.println("Maybe: " + Maybe.just(pass));
		return Maybe.just(pass);
	}

	public static boolean sendMessage(Stored<User> sender, String recipient, String content) {
		try{
			Message msg = new Message(sender.getValue(), recipient, content);
			sender.getValue().addMessage(msg);

			System.out.println("melding lagret: " + sender.getValue().getMessages());
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
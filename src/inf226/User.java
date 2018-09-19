package inf226;

import java.util.ArrayList;

/**
 * Immutable class for users.
 * @author INF226
 *
 */
public final class User {
	
	private final String name;
	private final String password;
	private final ImmutableLinkedList<Message> log;
	private ArrayList<User> users;


	public User(final String name, String password) {
		this.name=name;
		this.log = new ImmutableLinkedList<Message>();
		this.users = new ArrayList<>();
		this.password = password;
	}

	private User(final String name, String password, final ImmutableLinkedList<Message> log) {
		this.name=name;
		this.password = password;
		this.log = log;
	}

	/**
	 *
	 */
	public void addUserToList(User u){
		users.add(u);
	}

	/**
	 *
	 */
	public void checkUserList(){
		for(int i = 0; i < users.size(); i++){
			System.out.println("userlist: " + users.get(i).getName());
		}
	}
	
	/**
	 * 
	 * @return User name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return Messages sent to this user.
	 */
	public Iterable<Message> getMessages() {
		return log;
	}



	/**
	 * Add a message to this user’s log.
	 * @param m Message
	 * @return Updated user object.
	 */
	public User addMessage(Message m) {
		return new User(name, password, new ImmutableLinkedList<Message>(m,log));
	}

}

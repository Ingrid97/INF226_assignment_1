package inf226;

/**
 * Immutable class for users.
 * @author INF226
 *
 */
public final class User {
	private final String name;
	private final String password;
	private final ImmutableLinkedList<Message> log;


	public User(final String name, String password) {
		this.name=name;
		this.log = new ImmutableLinkedList<Message>();
		this.password = password;
	}

	private User(final String name, String password, final ImmutableLinkedList<Message> log) {
		this.name=name;
		this.password = password;
		this.log = log;
	}
	
	/**
	 * 
	 * @return User name
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 * @return password
	 */
	public String getPassword(){ return password; }
	
	/**
	 * @return Messages sent to this user.
	 */
	public Iterable<Message> getMessages() {
		return log;
	}


	public int getSize(){
		return log.getSize();
	}


	/**
	 * Add a message to this user’s log.
	 * @param m Message
	 * @return Updated user object.
	 */
	public User addMessage(Message m) {
		System.out.println("message added to list!");
		return new User(name, password, new ImmutableLinkedList<Message>(m,log));
	}

}

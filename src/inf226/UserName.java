package inf226;

public class UserName implements Comparable{
    String username;

    public UserName(String name) throws Exception {

        this.username = Server.validateUsername(name).force();
    }

    @Override
    public int compareTo(Object o) {
        UserName other = (UserName) o;
        return this.username.compareTo(other.username);
    }
}

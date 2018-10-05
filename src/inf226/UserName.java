package inf226;

public class UserName implements Comparable{
    String username;

    public UserName(String name) throws Exception {

        if (validateUsername(name)) {
            this.username = name;
        } else {
            throw new Exception();
        }

    }

    private static boolean validateUsername(String username) {
        if (username.matches("^[a-zA-Z0-9]+"))
            return true;
        return false;
    }

    @Override
    public int compareTo(Object o) {
        UserName other = (UserName) o;
        return this.username.compareTo(other.username);
    }
}

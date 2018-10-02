package inf226;

import java.io.IOException;

public class UserName {
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

}

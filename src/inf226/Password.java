package inf226;

public class Password {
    String password;

    public Password(String pass) throws Exception {

        if (validatePassword(pass)) {
            this.password = pass;
        } else {
            throw new Exception();
        }
    }

    private static boolean validatePassword(String pass) {
        if (pass.matches("^[a-zA-Z0-9]+"))
            return true;
        return false;
    }
}

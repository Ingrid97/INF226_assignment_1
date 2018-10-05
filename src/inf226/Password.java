package inf226;

public class Password {
    String password;

    public Password(String pass) throws Exception {

        this.password = Server.validatePassword(pass).force();
    }

    //TODO: sikkerhetsdelen .-.
}

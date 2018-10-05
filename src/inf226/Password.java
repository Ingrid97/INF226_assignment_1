package inf226;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Password {
    private final String password;
    private final String salt;

    public Password(String pass, String salt) throws Maybe.NothingException {

        pass = Server.validatePassword(pass).force();

        pass += salt;
        MessageDigest digest = null;
        String newPass = "";
        try {

            digest = MessageDigest.getInstance("SHA-512");
            byte[] encodedhash = digest.digest(pass.getBytes(StandardCharsets.UTF_8));

            for(byte b : encodedhash)
                newPass += (char)b;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        this.password = newPass;
        this.salt = salt;

    }

    public Password(String pass){
        this.password = pass;
        salt = "";
    }

    @Override
    public String toString() {
        return this.password.toString();
    }

    public String getSalt() {
        return salt;
    }

    @Override
    public boolean equals(Object obj) {
        Password pass = (Password) obj;
        return this.password.equals(pass.toString());
    }
}

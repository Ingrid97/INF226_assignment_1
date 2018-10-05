package inf226;

import inf226.Storage.Id;
import inf226.Storage.KeyedStorage;
import inf226.Storage.Storage;
import inf226.Storage.Stored;

import java.io.IOException;
import java.sql.*;
import java.util.TreeMap;
import java.util.function.Function;

public class DataBaseUserStorage implements KeyedStorage<UserName, User> {
//public class DataBaseUserStorage<K,C> implements KeyedStorage<K,C>{

    private final Id.Generator id_generator;
    private final TreeMap<Id,Stored<User> > memory;
    private final TreeMap<UserName,Id> keytable;
    private static DataBaseUserStorage single_instance = null;

    private static Connection conn = null;
    private static String url = "jdbc:sqlite:ourDB.db";
    private static int i = 1;
    //private final Function<User,UserName> computeKey;

    public DataBaseUserStorage(){
        //final Function<User,UserName> computeKey
        //this.computeKey = computeKey;

        id_generator = new Id.Generator();
        memory = new TreeMap<Id,Stored<User>>();
        keytable = new TreeMap<UserName,Id>();
    }

    public static DataBaseUserStorage getInstance() {
        if (single_instance == null)
            single_instance = new DataBaseUserStorage();

        return single_instance;
    }

    /**
     * @param args the command line arguments
     * masse tester for å sjekke om databasen fungerer!!!
     */
    public static void main(String[] args) {

        //for å sjekke connection til databasen
        //oppretter databasen om den ikke eksisterer allerede
        //brukes i main på Server
        //connect();
        //makeTable();


        /**
         * tester av database, IKKE til bruk i oppgaven
         */

        //test for adding
        //addUser("ijo031","123");

        //test for sletting
        //deleteUser("ijoo31");

        //test for henting av passord/user(ikke noe ferdig)
        //getUser("ijo031", "12f3");
    }


    public void connect() {

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
            System.out.println("Connection been established.");


        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public void makeTable(){

        // SQL statement for creating a new table
        String sqlusers = "CREATE TABLE IF NOT EXISTS users (\n"
                + "	Id int,\n"
                + "	UserName varchar(255),\n"
                + "	Password varchar(255)\n"
                + ");";

        String sqlmessage = "CREATE TABLE IF NOT EXISTS messages (\n"
                + "	Id int,\n"
                + "	SenderName varchar(255),\n"
                + "	ReciverName varchar(255),\n"
                + "	Messaeg varchar(255)\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // create a new table
            stmt.execute(sqlusers);
            stmt.execute(sqlmessage);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public static void addUser(String user, String Password){

        String s = "INSERT INTO users(Id, UserName, Password) \n"
                + "VALUES ('" + i++ + "', '"+ user + "', '" + Password + "');";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(s);
            System.out.println("user " + user  +" added to the database!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void deleteUser(String user){

        String s = "DELETE FROM users\n"
                + "WHERE UserName ='" + user + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(s);
            System.out.println("user " + user  +" deleted from the database!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void getUser(String user, String password){

        String sql = "SELECT UserName FROM users WHERE UserName ='" + user + "' AND Password = '" + password + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            if(rs.isClosed()){
                System.out.println("heihei");
            }
            System.out.println(rs.getString("UserName"));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Maybe<Stored<User>> lookup(Id key) {
        return new Maybe<>(memory.get(key));
    }


    @Override
    public Maybe<Stored<User>> lookup(UserName resipient) {
        Maybe<Id> id = new Maybe<>(keytable.get(resipient));
        System.out.println("lookup id: " + id.getValue());

        //check if user in storage
        if (id.isNothing()){
            System.err.println("Key not in store " + resipient);
            String name = resipient.username;
            String sql = "SELECT UserName, Password  FROM users WHERE UserName ='" + name + "';";
            //String sqlMessage = "SELECT SenderName, ReciverName, Messaeg FROM messages WHERE ReciverName = '" + name + "'";

            //connect and get from database
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt  = conn.createStatement();
                 ResultSet rs    = stmt.executeQuery(sql)){

                //get username and password
                String sender = rs.getString("UserName");
                String password = rs.getString("Password");

                //make the user
                User tempUser = new User(resipient.username, password);

                User user = addMessages(tempUser);
                System.out.println(user.getMessages().iterator().hasNext());

                //make stored user
                Id.Generator generator = new Id.Generator();
                Stored<User> newU = new Stored<User>(generator, user);

                Stored<User> stored = new Stored<>(id_generator, user);
                memory.put(stored.id(), stored);
                keytable.put(stored.getValue().getUserName(), stored.id());

                //return the user
                return Maybe.just(newU);

            } catch (SQLException e) {
                System.out.println(e.getMessage());
                return Maybe.nothing();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //if the user is in storage
        try {
            System.out.println("LOOKING UP THE KEY FROM THE USER " + id.toString());
            return lookup(id.force());
        } catch (Maybe.NothingException e) {
            return Maybe.nothing();
        }
    }

    public User addMessages(User reciver) {
        String sqlMessage = "SELECT SenderName, Messaeg FROM messages WHERE ReciverName = '" + reciver.getName() + "'";

        //connect and get from database
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlMessage)) {

            User newUser = reciver;

            // loop through the result set
            while (rs.next()) {
                String message_sender = rs.getString("SenderName");
                String message = rs.getString("Messaeg");

                System.out.println(reciver.getName());
                User sender = new User(message_sender, "123");

                System.out.println(message);
                Message m = new Message(sender, reciver.getName(), message);
                newUser = newUser.addMessage(m);
                System.out.println("message added to the user!");
            }
            System.out.println(newUser.getMessages().iterator().hasNext());
            return newUser;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (Message.Invalid invalid) {
            invalid.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Stored<User> save(User value) throws IOException {

        User u = (User) value;

        //SQL query
        String s = "INSERT INTO users (id, UserName, Password)\n"
                + "VALUES ('" + i++ + "', '"+ u.getName() + "', '" + u.getPassword() + "');";

        //connect to database
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // create a new user
            stmt.execute(s);
            System.out.println("user " + u.getName()  +" added to the database!");
            Id.Generator generator = new Id.Generator();
            Stored<User> newU = new Stored<>(generator, value);

            //stuff fra TransistentStorrage
            memory.put(newU.id(),newU);
            //keytable.put(computeKey.apply(value), newU.id());
            keytable.put(u.getUserName(), newU.id());

            return newU;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Stored<User> refresh(Stored<User> old) throws ObjectDeletedException, IOException {
        Stored<User> newValue = memory.get(old.id());
        if(newValue == null)
            throw new ObjectDeletedException(old.id());
        return newValue;
    }

    @Override
    public Stored<User> update(Stored<User> old, User newValue) throws ObjectModifiedException, ObjectDeletedException, IOException {

        String sender = newValue.getMessages().iterator().next().sender;
        String message = newValue.getMessages().iterator().next().message;

        String s = "INSERT INTO messages (Id, SenderName, ReciverName, Messaeg)\n"
                + "VALUES ('" + i++ + "', '"+ sender + "', '" + newValue.getName() + "', '" + message + "');";

        System.out.println("sender: " + sender);
        System.out.println("reciver: " + newValue.getName());
        System.out.println("message: " + message);

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(s);
            System.out.println("massage added to the database!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        Stored<User> stored = memory.get(old.id());
        if(stored == null) {
            throw new Storage.ObjectDeletedException(old.id());
        }

        if (!stored.equals(old)) {
            throw new Storage.ObjectModifiedException(stored);
        }
        Stored<User> newStored = new Stored<User>(old,newValue);
        memory.put(old.id(), newStored);
        return newStored;
    }

    @Override
    public void delete(Stored<User> old) throws ObjectModifiedException, ObjectDeletedException, IOException {

        String s = "DELETE FROM users\n"
                + "WHERE UserName ='" + old.getValue() + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(s);
            System.out.println("user " + old.getValue()  +" deleted from the database!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

}

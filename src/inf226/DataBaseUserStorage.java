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

    private DataBaseUserStorage(){
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
     * connects to the database, create if does not exist
     */

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

    /**
     * create the tables in the database
     */

    public void makeTable(){

        // SQL statement for creating a new table
        String sqlusers = "CREATE TABLE IF NOT EXISTS users (\n"
                + "	UserName varchar(255),\n"
                + "	Password varchar(255),\n"
                + " Salt char(64)\n"
                + ");";

        String sqlmessage = "CREATE TABLE IF NOT EXISTS messages (\n"
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

    public Maybe<Stored<User>> lookup(Id key) {
        return new Maybe<>(memory.get(key));
    }

    @Override
    public Maybe<Stored<User>> lookup(UserName resipient) {
        Maybe<Id> id = new Maybe<>(keytable.get(resipient));
        System.out.println("lookup id: " + id.getValue());

        //check if user in storage
        if (id.isNothing()){
            System.out.println("database");
            System.err.println("Key not in store " + resipient);
            String sql = "SELECT Password, Salt  FROM users WHERE UserName = ?;";

            //connect and get from database
            try {
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, resipient.username);
                ResultSet rs    = stmt.executeQuery();

                //get password and salt
                String password = rs.getString("Password");
                String salt = rs.getString("Salt");

                //make the user
                User tempUser = new User(resipient.username, password, salt);

                //test Data
                System.out.println("UserName: " + tempUser.getName());


                User user = addMessages(tempUser);
                System.out.println(user.getMessages().iterator().hasNext());

                //make stored user

                Stored<User> stored = new Stored<>(id_generator, user);
                memory.put(stored.id(), stored);
                keytable.put(stored.getValue().getUserName(), stored.id());

                //return the user
                return Maybe.just(stored);

            } catch (SQLException e) {
                System.out.println(e.getMessage());
                return Maybe.nothing();
            } catch (Exception e) {
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

        //if the user is in storage
        try {
            System.out.println("LOOKING UP THE KEY FROM THE USER " + id.toString());
            return lookup(id.force());
        } catch (Maybe.NothingException e) {
            return Maybe.nothing();
        }
    }

    public User addMessages(User reciver) {
        String sqlMessage = "SELECT SenderName, Messaeg FROM messages WHERE ReciverName = ?;";

        //connect and get from database
        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement stmt = conn.prepareStatement(sqlMessage);
            stmt.setString(1, reciver.getName());
            ResultSet rs = stmt.executeQuery();

            User newUser = reciver;

            // loop through the result set
            while (rs.next()) {
                String message_sender = rs.getString("SenderName");
                String message = rs.getString("Messaeg");

                System.out.println(reciver.getName());
                User sender = new User(message_sender, "123", "salt");

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

    @Override
    public synchronized Stored<User> save(User value) throws IOException {

        User u = value;

        //SQL query
        String sqlInsert = "INSERT INTO users ( UserName, Password, Salt)\n"
                + "VALUES (?, ?, ?);";
        //connect to database
        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement stmt = conn.prepareStatement(sqlInsert);
            stmt.setString(1, u.getName());
            stmt.setString(2, u.getPassword());
            stmt.setString(3, u.getSalt());

            // create a new user
            stmt.execute();
            System.out.println("user " + u.getName()  +" added to the database!");
            Stored<User> newU = new Stored<>(id_generator, value);

            //stuff fra TransistentStorrage
            memory.put(newU.id(),newU);
            keytable.put(newU.getValue().getUserName(), newU.id());

            return newU;
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
        return null;
    }

    @Override
    public synchronized Stored<User> refresh(Stored<User> old) throws ObjectDeletedException, IOException {
        Stored<User> newValue = memory.get(old.id());
        if(newValue == null)
            throw new ObjectDeletedException(old.id());
        return newValue;
    }

    @Override
    public synchronized Stored<User> update(Stored<User> old, User newValue) throws ObjectModifiedException, ObjectDeletedException, IOException {


        String sender = newValue.getMessages().iterator().next().sender;
        String reciver = newValue.getMessages().iterator().next().recipient;
        String message = newValue.getMessages().iterator().next().message;

        String sqlInsert = "INSERT INTO messages (SenderName, ReciverName, Messaeg)\nVALUES (?, ?, ?);";

        System.out.println("sender: " + sender);
        System.out.println("reciver: " + newValue.getName() + " : " + reciver);
        System.out.println("message: " + message);

        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement stmt = conn.prepareStatement(sqlInsert);
            stmt.setString(1, sender);
            stmt.setString(2, reciver);
            stmt.setString(3, message);

            // create a new table
            stmt.execute();
            System.out.println("massage added to the database!");
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

        Stored<User> stored = memory.get(old.id());
        if(stored == null) {
            throw new Storage.ObjectDeletedException(old.id());
        }

        if (!stored.equals(old)) {
            throw new Storage.ObjectModifiedException(stored);
        }
        Stored<User> newStored = new Stored<User>(stored,newValue);
        memory.put(stored.id(), newStored);

        return newStored;
    }

    @Override
    public synchronized void delete(Stored<User> old) throws ObjectModifiedException, ObjectDeletedException, IOException {

        String sqlDelete = "DELETE FROM users\nWHERE UserName =?;";

        try {
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement stmt = conn.prepareStatement(sqlDelete);
            stmt.setString(1, old.getValue().getName());
            // create a new table
            stmt.execute();
            System.out.println("user " + old.getValue()  +" deleted from the database!");
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
}

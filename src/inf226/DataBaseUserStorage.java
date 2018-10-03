package inf226;

import inf226.Storage.Id;
import inf226.Storage.KeyedStorage;
import inf226.Storage.Storage;
import inf226.Storage.Stored;

import java.io.IOException;
import java.sql.*;
import java.util.TreeMap;
import java.util.function.Function;

public class DataBaseUserStorage<K,C> implements KeyedStorage<K,C> {

    private final Id.Generator id_generator;
    private final TreeMap<Id,Stored<C> > memory;
    private final TreeMap<K,Id> keytable;

    private static Connection conn = null;
    private static String url = "jdbc:sqlite:ourDB.db";
    private static int i = 1;
    private final Function<C,K> computeKey;

    public DataBaseUserStorage(final Function<C,K> computeKey){
        this.computeKey = computeKey;

        id_generator = new Id.Generator();
        memory = new TreeMap<Id,Stored<C>>();
        keytable = new TreeMap<K,Id>();
    }

    /**
     * @param args the command line arguments
     * masse tester for å sjekke om databasen fungerer!!!
     */
    public static void main(String[] args) {

        //for å sjekke connection til databasen
        //oppretter databasen om den ikke eksisterer allerede
        //connect();

        //for å lage table users i databasen
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


    public static void connect() {

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

    public static void makeTable(){

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                + "	Id int,\n"
                + "	UserName varchar(255),\n"
                + "	Password varchar(255)\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
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

        //INSERT INTO Customers (CustomerName, ContactName, Address, City, PostalCode, Country)
        //VALUES ('Cardinal', 'Tom B. Erichsen', 'Skagen 21', 'Stavanger', '4006', 'Norway');
        String s = "INSERT INTO users (id, UserName, Password)\n"
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

        //DELETE FROM Customers
        //WHERE CustomerName='Alfreds Futterkiste';
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

    public Maybe<Stored<C>> lookup(Id key) {
        return new Maybe<>(memory.get(key));
    }


    @Override
    public Maybe<Stored<C>> lookup(K key) {
        Maybe<Id> id = new Maybe<>(keytable.get(key));

        //check if user in storage
        if (id.isNothing()){
            System.err.println("Key not in store " + key);
            String name = (String)key;
            String sql = "SELECT UserName, Password  FROM users WHERE UserName ='" + name + "';";

            //connect and get from database
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt  = conn.createStatement();
                 ResultSet rs    = stmt.executeQuery(sql)){

                //get username and password
                String username = rs.getString("UserName");
                String password = rs.getString("Password");

                //make the user
                User user = new User(username, password);

                //make stored user
                Id.Generator generator = new Id.Generator();
                Stored<C> newU = new Stored<C>(generator, (C)user);

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

    @Override
    public Stored<C> save(C value) throws IOException {

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
            Stored<C> newU = new Stored<>(generator, value);

            //stuff fra TransistentStorrage
            memory.put(newU.id(),newU);
            keytable.put(computeKey.apply(value), newU.id());

            return newU;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public Stored<C> refresh(Stored<C> old) throws ObjectDeletedException, IOException {
        return null;
    }

    @Override
    public Stored<C> update(Stored<C> old, C newValue) throws ObjectModifiedException, ObjectDeletedException, IOException {

        //TODO: kan bare bruke delete(old) først og så save(newValue) (tror eg)
        return null;
    }

    @Override
    public void delete(Stored<C> old) throws ObjectModifiedException, ObjectDeletedException, IOException {

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

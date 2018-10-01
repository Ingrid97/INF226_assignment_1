package inf226;

import java.io.BufferedReader;

import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import inf226.Maybe.NothingException;
import inf226.Storage.Stored;
import inf226.Storage.TransientStorage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This class handles the requests from clients.
 *
 * @author INF226
 *
 */
public final class RequestProcessor extends Thread {
    private final BlockingQueue<Request> queue;

    public RequestProcessor() {
        queue = new LinkedBlockingQueue<Request>();
    }

    /**
     * Add a request to the queue.
     * @param request
     * @return
     */
    public boolean addRequest(final Request request) {
        return queue.add(request);
    }

    public void run() {
        try {
            while(true) {
                final Request request = queue.take();
				/*
				 * TODO: Implement mitigation against a flood
				 * of requests from a single host by keeping
				 * track of the number of requests per host.
				 */
                request.start();
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * The type of requests.
     * @author INF226
     *
     */
    public static final class Request extends Thread{
        private final Socket client;
        private Maybe<Stored<User>> user;

        /**
         * Create a new request from a socket connection to a client.
         * @param client Socket to communicate with the client.
         */
        public Request(final Socket client) {
            this.client = client;
            user = Maybe.nothing();
        }

        @Override
        public void run() {
            try(final BufferedWriter out =
                        new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                final BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()))) {
                while(true) {
                    handle(in,out);
                }
            } catch (IOException e) {
                // Client disconnected
            }
            try {
                client.close();
            } catch (IOException e) {
                // Client closed.
            }
        }

        /**
         * Handle a single request
         * @param in Input from the client.
         * @param out Output to the client.
         * @throws IOException If the user hangs up unexpectedly
         */
        private void handle(final BufferedReader in, final BufferedWriter out) throws IOException {
            final String requestType = Util.getLine(in);

            if(requestType.equals("REQUEST TOKEN")) {
                try {
                    final Token token = Server.createToken(user.force()).force();
                    out.write("TOKEN " + token.stringRepresentation());
                } catch (NothingException e) {
                    out.write("FAILED");
                }
                out.newLine();
                out.flush();
                return;
            }
            if(requestType.equals("REGISTER")) {
                System.err.println("Handling registration request");
                user = handleRegistration(in);
                try {
                    out.write("REGISTERED " + user.force().getValue().getName());
                    System.err.println("Registration request succeeded.");
                } catch (NothingException e) {
                    out.write("FAILED");
                    System.err.println("Registration request failed.");
                }
                out.newLine();
                out.flush();
                return;
            }
            if(requestType.equals("LOGIN")) {
                user = handleLogin(in);
                try {
                    out.write("You are now logged in, " + user.force().getValue().getName() + " \n");
                } catch (NothingException e) {
                    out.write("Login failed. Try again.\n");
                }
                out.newLine();
                out.flush();
                return;
            }
            if(requestType.equals("SEND MESSAGE")) {
                try {
                    user = Server.refresh(user.force());
                    final Maybe<Message> message = handleMessage(user.force().getValue().getName(),in);

                    if(Server.sendMessage(user.getValue(),message.getValue().recipient, message.getValue().message)) {
                        out.write("MESSAGE SENT");
                        System.err.println("Message has been sent.");
                        out.write("test");
                    } else {
                        out.write("FAILED");

                    }
                } catch (NothingException e) {
                    out.write("FAILED");
                }
                out.newLine();
                out.flush();
                return;
            }
            if(requestType.equals("READ MESSAGES")) {
                System.err.println("Handling a read message request");
                try {
                    // Refresh the user object in order to get new messages.
                    user = Server.refresh(user.force());
                    for (Message m : user.force().getValue().getMessages()) {
                        System.err.println("Sending message from " + m.sender);
                        out.write("MESSAGE FROM " + m.sender); out.newLine();
                        out.write(m.message);out.newLine();
                        out.write(".");out.newLine();
                        out.flush();
                    }
                    out.write("END OF MESSAGES");

                } catch (NothingException e) {
                    out.write("FAILED");
                }
                out.newLine();
                out.flush();
                return;
            }
        }

        /**
         * Handle a message send request
         * @param username The name of the user sending the message.
         * @param in Reader to read the message data from.
         * @return Message object.
         */
        private  Maybe<Message> handleMessage(String username, BufferedReader in) throws IOException{
            final String lineOne = Util.getLine(in);
            final String lineTwo = Util.getLine(in);

            if (lineOne.startsWith("RECEIVER ") && lineTwo.startsWith("MESSAGE ")) {
                final String receiver = lineOne.substring("RECEIVER ".length(), lineOne.length()); // DENNE VIRKER
                final String message = lineTwo.substring("MESSAGE ".length(), lineTwo.length()); // DENNE VIRKER

                try {
                    user = Server.refresh(user.force());

                    System.out.println("TESTING. Username, sender: " + user.force().getValue().getName());
                    System.out.println("TESTING. Receiver: " + receiver);
                    System.out.println("TESTING. Message: " + message);

                    Message msg = new Message(user.force().getValue(),receiver,message);
                    user.force().getValue().addMessage(msg);
                    System.out.println("Size of message list: " + user.force().getValue().getSize());
                    System.out.println("Messages sent to the user : " + user.force().getValue().getMessages());


                    return Maybe.just(msg);

                }
                catch (NothingException e) {
                    e.printStackTrace();
                }
                catch (Message.Invalid i){

                }

            }
            else {
                return Maybe.nothing();
            }
            return Maybe.nothing();
        }

        /**
         * Handle a registration request.
         * @param in Request input.
         * @return The stored user as a result of the registration.
         * @throws IOException If the client hangs up unexpectedly.
         */
        private static Maybe<Stored<User>> handleRegistration(BufferedReader in) throws IOException {
            final String lineOne = Util.getLine(in);
            final String lineTwo = Util.getLine(in);

            if (lineOne.startsWith("USER ") && lineTwo.startsWith("PASS ")) {
                final Maybe<String> username = Server.validateUsername(lineOne.substring("USER ".length(), lineOne.length()));
                final Maybe<String> password = Server.validatePassword(lineTwo.substring("PASS ".length(), lineTwo.length()));

                try {
                    return Server.register(username.force(), password.force());
                } catch (NothingException e) {
                    return Maybe.nothing();
                }
            } else {
                return Maybe.nothing();
            }
        }

        /**
         * Handle a login request.
         * @param in Request input.
         * @return User object as a result of a successful login.
         * @throws IOException If the user hangs up unexpectedly.
         */
        private static Maybe<Stored<User>> handleLogin(final BufferedReader in) throws IOException {
            final String lineOne = Util.getLine(in);
            final String lineTwo = Util.getLine(in);

            if (lineOne.startsWith("USER ") && lineTwo.startsWith("PASS ")) {
                final Maybe<String> username = Server.validateUsername(lineOne.substring("USER ".length(), lineOne.length()));
                final Maybe<String> password = Server.validatePassword(lineTwo.substring("PASS ".length(), lineTwo.length()));

                try {
                    System.err.println("Login request from user: " + username.force());
                    return Server.authenticate(username.force(), password.force());
                } catch (NothingException e) {
                    return Maybe.nothing();
                }
            } else {
                return Maybe.nothing();
            }
        }
    }
}

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Scanner;

public class Server {
    private static final int PORT = 8080;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is running and waiting for connections...");

            // Thread for server admin input
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String serverMessage = scanner.nextLine();
                    broadcast("[Server]: " + serverMessage, null);
                }
            }).start();

            // Accept incoming clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast message to all clients
    public static void broadcast(String message, ClientHandler sender) {
        String timeStampedMessage = "[" + timestamp() + "] " + message;
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(timeStampedMessage);
            }
        }
    }

    // List all active users
    public static String listUsers() {
        StringBuilder sb = new StringBuilder("Active Users:\n");
        for (ClientHandler client : clients) {
            sb.append(" - ").append(client.getUsername()).append("\n");
        }
        return sb.toString();
    }

    // Get formatted timestamp
    private static String timestamp() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // Internal class for client handling
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out.println("Enter your username:");
                username = in.readLine();

                System.out.println("User " + username + " connected.");
                broadcast("🔔 " + username + " has joined the chat!", this);

                out.println("Welcome, " + username + "! Type /exit to leave, /list to see users.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("/")) {
                        handleCommand(inputLine);
                    } else {
                        System.out.println("[" + username + "]: " + inputLine);
                        broadcast("[" + username + "]: " + inputLine, this);
                    }
                }

            } catch (IOException e) {
                System.out.println("User " + username + " disconnected unexpectedly.");
            } finally {
                clients.remove(this);
                broadcast(username + " has left the chat.", this);
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Handle client commands
        private void handleCommand(String cmd) {
            switch (cmd) {
                case "/exit":
                    sendMessage("Goodbye " + username + "!");
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "/list":
                    sendMessage(Server.listUsers());
                    break;
                default:
                    sendMessage("Unknown command: " + cmd);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public String getUsername() {
            return username;
        }
    }
}

import java.io.*;
import java.net.*;

public class MessagingLibrary {
    private static int rank;
    private static int size;
    private static ServerSocket serverSocket;
    private static Socket[] client_connections;
    private static int[] ports;

    // Initialize the messaging library
    public static void init(int rank, int size, int port, int[] ports) throws IOException {
        MessagingLibrary.rank = rank;
        MessagingLibrary.size = size;
        MessagingLibrary.ports = ports;
        MessagingLibrary.client_connections = new Socket[size];
        System.out.printf("Initializing MessagingLibrary for rank %d on port %d%n", rank, port);

        // Create ServerSocket if the current rank owns the port
        if (!isPortUsed(port)) {
            serverSocket = new ServerSocket(port);
            System.out.printf("ServerSocket created for rank %d on port %d%n", rank, port);

            // Accept client_connections
            new Thread(() -> {
                try {
                    for (int i = 0; i < size; i++) {
                        if (i != rank) {
                            Socket socket = serverSocket.accept();
                            client_connections[i] = socket;
                            System.out.printf("Rank %d accepted connection from rank %d%n", rank, i);
                        }
                    }
                } catch (IOException e) {
                    System.err.printf("Error accepting client_connections for rank %d: %s%n", rank, e.getMessage());
                }
            }).start();
        }

        // Connect to other processes
        for (int i = 0; i < size; i++) {
            if (i != rank) {
                try {
                    System.out.printf("Rank %d connecting to rank %d on port %d%n", rank, i, ports[i]);
                    Socket socket = new Socket("localhost", ports[i]);
                    client_connections[i] = socket;
                } catch (IOException e) {
                    System.err.printf("Error connecting to rank %d: %s%n", i, e.getMessage());
                }
            }
        }
    }

    // Check if a port is already used
    private static boolean isPortUsed(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    // Returns the total number of processes
    public static int comm_size() {
        return size;
    }

    // Returns the rank of the current process
    public static int comm_rank() {
        return rank;
    }

    // Send a message to a specific process
    public static void send(int dest, String message) throws IOException {
        System.out.printf("Rank %d sending message to rank %d: %s%n", rank, dest, message);
        Socket socket = client_connections[dest];
        if (socket != null) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } else {
            System.err.printf("Connection to rank %d not established.%n", dest);
        }
    }

    // Receive a message from any process
    public static String receive() throws IOException {
        for (int i = 0; i < size; i++) {
            if (i != rank && client_connections[i] != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(client_connections[i].getInputStream()));
                if (in.ready()) {
                    return in.readLine();
                }
            }
        }
        return null;
    }

    // Close all client_connections
    public static void close() throws IOException {
        System.out.printf("Closing client_connections for rank %d%n", rank);
        for (Socket socket : client_connections) {
            if (socket != null) {
                socket.close();
            }
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}

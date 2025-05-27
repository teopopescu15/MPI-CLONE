import java.io.IOException;

public class program {
    public static void main(String[] args) {
        if (args.length < 4) { // Adjusted for receiving the full list of ports
            System.err.println("Error of arguments: java program <rank> <size> <port> <port_0> <port_1> ... <port_N>");
            System.exit(1);
        }

        int rank = Integer.parseInt(args[0]);
        int size = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);

        // Parse all ports into an array
        int[] ports = new int[size];
        for (int i = 0; i < size; i++) {
            ports[i] = Integer.parseInt(args[3 + i]);
        }

        try {
            System.out.printf("Starting process %d with size %d on port %d%n", rank, size, port);

            MessagingLibrary.init(rank, size, port, ports);
            System.out.printf("Process %d initialized on port %d%n", rank, port);

            if (rank == 0) {
                // Send a message to all other ranks
                for (int i = 1; i < size; i++) {
                    String message = "Hello from rank 0 to rank " + i;
                    System.out.printf("Rank %d sending message to rank %d: %s%n", rank, i, message);
                    MessagingLibrary.send(i, message);
                }
            } else {
                // Receive a message
                String message = MessagingLibrary.receive();
                if (message != null) {
                    System.out.printf("Rank %d received message: %s%n", rank, message);
                }
            }

            // Close client_connections
            MessagingLibrary.close();
            System.out.printf("Process %d closed client_connections%n", rank);

        } catch (IOException e) {
            System.err.printf("Error in process %d: %s%n", rank, e.getMessage());
        }
    }
}

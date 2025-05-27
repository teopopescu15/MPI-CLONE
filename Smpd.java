import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Smpd {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Error:arguments not displayed correctly");
            System.exit(1);

        }

        int portNumber;
        try {
            portNumber = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
            return;
        }

        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.printf("Server started on port %d%n", portNumber);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.printf("New client connection on port %d%n", portNumber);
                    threadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    System.err.printf("Error accepting client connection: %s%n", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("Error creating server: %s%n", e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                // Read the initial command sent from mpiexec
                String command = in.readLine();
                if (command == null || command.trim().isEmpty()) {
                    out.println("Error: No command received.");
                    return;
                }

                System.out.printf("Command received: '%s'%n", command);

                // Check if the command is to start a program or MessageLibrary-Program
                // communication

                if (command.startsWith("java program")) {
                    String[] commandParts = command.split(" ");
                    ProcessBuilder processBuilder = new ProcessBuilder(commandParts);

                    // C
                    processBuilder.redirectErrorStream(true);

                    // Start the process
                    Process process = processBuilder.start();
                    System.out.printf("Process started: %s%n", command);

                    // Forward program output back to the mpiexec
                    try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = output.readLine()) != null) {
                            out.println(line);
                        }
                    }

                    int exitCode = process.waitFor();
                    out.println("Process finished with exit code " + exitCode);
                } else {
                    // Handle Message-Program communication
                    System.out.println("Inter-process message received: " + command);
                    out.println("Message received: " + command);
                }

            } catch (IOException | InterruptedException e) {
                System.err.printf("Error handling client: %s%n", e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.printf("Error closing clientSocket: %s%n", e.getMessage());
                }
            }
        }
    }
}

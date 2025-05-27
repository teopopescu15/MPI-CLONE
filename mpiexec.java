import java.io.*;
import java.net.*;
import java.util.concurrent.*;

//-hosts
public class mpiexec {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Argument incorrect:java mpiexec -processes N port_1 port_2 .... port_N program.exe");
            System.err.println(
                    "Argument incorrect:java mpiexec -hosts  N  IPADDRESS_1 IPADDRESS_2 ....  IPADDRESS_N program.exe");
            System.exit(1);
        }

        int number = Integer.parseInt(args[1]); // args[1] este "3"
        if (args.length < number + 3) {
            System.err.println("Not enough arguments for the specified number of processes");
            System.exit(1);
        }

        int[] portNumber = new int[number];
        for (int i = 0; i < number; i++) {
            portNumber[i] = Integer.parseInt(args[i + 2]);
            System.out.printf("Port for process %d is %d%n", i, portNumber[i]);
        }
        if (args[0].equals("-processes")) {
            ProcessCase(args, portNumber, number);
        } else if (args[0].equals("-hosts")) {
            HostCase(args, number);
        } else {
            System.err.println("Args[0] not -process or -hosts");
            System.exit(1);

        }
    }

    private static void ProcessCase(String[] args, int[] portNumber, int number) {

        String program = args[number + 2] + " " + args[number + 3];
        System.out.printf("Program is '%s'%n", program);

        ExecutorService threadPool = Executors.newFixedThreadPool(number);
        // java program 0 3 5000 5000 5001 5002
        for (int i = 0; i < number; i++) {
            StringBuilder commandBuilder = new StringBuilder(
                    String.format("%s %d %d %d", program, i, number, portNumber[i]));
            for (int port : portNumber) {
                commandBuilder.append(" ").append(port);
            }
            String command = commandBuilder.toString();
            System.out.printf("Executing command for process %d: %s%n", i, command);
            threadPool.submit(new ClientHandler("localhost", portNumber[i], command));
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("ThreadPool Error: Some tasks did not finish in time.");
            }
        } catch (InterruptedException e) {
            System.err.println(" ThreadPool interrupted: " + e.getMessage());
        }
    }

    private static void HostCase(String[] args, int number) {

        String program = args[number + 2] + " " + args[number + 3];
        String[] hosts = new String[number];
        System.out.printf("Program is '%s'%n", program);
        int port = 5000;
        ExecutorService threadPool = Executors.newFixedThreadPool(number);
        for (int i = 0; i < number; i++) {
            hosts[i] = args[i + 2];
        }

        // java program 0 3 5000 5000 5001 5002
        for (int i = 0; i < number; i++) {
            StringBuilder commandBuilder = new StringBuilder(
                    String.format("%s %d %d %d", program, i, number, hosts[i]));
            for (String host : hosts) {
                commandBuilder.append(" ").append(host);
            }
            String command = commandBuilder.toString();
            System.out.printf("Executing command for process %d: %s%n", i, command);
            threadPool.submit(new ClientHandler(hosts[i], port, command));
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("ThreadPool Error: Some tasks did not finish in time.");
            }
        } catch (InterruptedException e) {
            System.err.println(" ThreadPool interrupted: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {

        private final int portNumber;
        private final String program;
        private final String host;

        public ClientHandler(String host, int portNumber, String program) {
            this.portNumber = portNumber;
            this.program = program;
            this.host = host;
        }

        public void run() {
            System.out.printf("Connecting to port %d with command '%s'%n", portNumber, program);
            try (Socket clientSocket = new Socket(host, portNumber);
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                out.println(program); // Sends the command to the server
                String inputLine;
                // Reads the response the server sent to the client
                while ((inputLine = in.readLine()) != null) {
                    System.out.printf(" Received from server on port %d: %s%n", portNumber, inputLine);
                }
            } catch (IOException e) {
                System.err.printf("Error connecting to server on port %d: %s%n", portNumber, e.getMessage());
            }
        }
    }

}
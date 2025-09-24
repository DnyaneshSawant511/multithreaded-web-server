import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Server {

    public Consumer<Socket> getConsumer() {
        return (clientSocket) -> {
            try {
                System.out.println("Connection accepted from Client " + clientSocket.getRemoteSocketAddress());

                PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true);
                toClient.println("Hello");

                toClient.close();
                clientSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public static void main(String[] args) {
        int port = 8010;
        int poolSize = 10;

        Server server = new Server();

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(15000);
            System.out.println("Server is listening on port " + port);

            while (true) {
                try {
                    Socket acceptedSocket = serverSocket.accept();
                    executor.submit(() -> server.getConsumer().accept(acceptedSocket));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 8010;
    private static final int POOL_SIZE = 30; 
    private static final String WEB_ROOT = "../www";

    private final ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            serverSocket.setSoTimeout(10000);
            System.out.println("HTTP Server running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String path = tokens[1];

            if (!method.equals("GET")) {
                sendResponse(out, "405 Method Not Allowed", "text/plain", "Only GET supported".getBytes());
                log(clientSocket, path, 405);
                return;
            }

            String filePath;
            switch (path) {
                case "/":
                    filePath = WEB_ROOT + "/index.html";
                    break;
                case "/about":
                    filePath = WEB_ROOT + "/about.html";
                    break;
                case "/contact":
                    filePath = WEB_ROOT + "/contact.html";
                    break;
                default:
                    filePath = WEB_ROOT + path;
            }

            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                //byte[] content = Files.readAllBytes(Paths.get(filePath));
                
                byte[] content;
                // Check if file is in cache
                if (Cache.contains(filePath)) {
                    content = Cache.get(filePath);
                    System.out.println("Cache HIT: " + filePath);
                } else {
                    content = Files.readAllBytes(Paths.get(filePath));
                    Cache.put(filePath, content);
                    System.out.println("Cache MISS: " + filePath);
                }

                String mimeType = getMimeType(filePath);
                sendResponse(out, "200 OK", mimeType, content);
                log(clientSocket, path, 200);
            } else {
                String notFound = "<h1>404 Not Found</h1>";
                sendResponse(out, "404 Not Found", "text/html", notFound.getBytes());
                log(clientSocket, path, 404);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void sendResponse(OutputStream out, String status, String contentType, byte[] content) throws IOException {
        PrintWriter writer = new PrintWriter(out);
        writer.println("HTTP/1.1 " + status);
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length);
        writer.println();
        writer.flush();

        out.write(content);
        out.flush();
    }

    private void log(Socket client, String path, int status) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String clientIp = client.getInetAddress().getHostAddress();
        System.out.printf("[%s] %s requested %s -> %d%n", time, clientIp, path, status);
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".ico")) {
            return "image/x-icon";
        } else {
            return "application/octet-stream";
        }
    }
}
package edu.cmu.ece;
import java.io.*;
import java.net.*;
import java.util.*;

public class VodServer {
    static final int MAX_CACHE_SIZE = 10;
    static final int MAX_OBJECT_SIZE = 102400;

    static Set<Integer> clients = new HashSet<>();
    static boolean closeOne = false;
    boolean abTest = true;
    static LRUCache<byte[]> cache = new LRUCache<>(MAX_CACHE_SIZE);

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        int port = Integer.valueOf(args[0]);
        int id = 0;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started listening on port: " + port + "...");

            // keep listening
            while (true) {
                try {
                    HttpServer server = new HttpServer(serverSocket.accept(), ++id);
                    // create thread to handle client
                    clients.add(id);
                    System.out.println("Establish a new connection.");
//                    System.out.println("Current client number: " + clients.size());
                    Thread thread = new Thread(server);
                    thread.start();
                } catch (IOException e) {
                    System.err.println("Accept failed.");
                    System.exit(1);
                }

            }
        } catch (IOException e) {
            System.err.println("Server could not listen on port: " + port + ".");
            System.exit(1);
        }
    }
}

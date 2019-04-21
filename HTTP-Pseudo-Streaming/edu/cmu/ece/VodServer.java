package edu.cmu.ece;
import java.io.*;
import java.net.*;
import java.util.*;

public class VodServer {
    static final int MAX_CACHE_SIZE = 10;
    static final int MAX_OBJECT_SIZE = Integer.MAX_VALUE;

    static Set<Integer> clients = new HashSet<>();
    static int mode = -1; // 0: normal(e.g. browser), 1: abTest with k, 2: abTest with no k
    static LRUCache<String, WebFile> cache = new LRUCache<>(MAX_CACHE_SIZE);

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        int port = Integer.valueOf(args[0]);
        while (mode < 0 || mode > 2) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Please enter the mode:\n 0: browser \n 1: ab test with -k\n 2: ab test without -k");
            mode = sc.nextInt();
        }
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
//                    System.out.println("Establish a new connection.");
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

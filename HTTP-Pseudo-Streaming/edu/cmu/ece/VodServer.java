package edu.cmu.ece;

import java.io.*;
import java.net.*;
import java.util.*;

public class VodServer {
    static Set<Integer> clients = new HashSet<>();
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
                    clients.add(id);
                    // create thread to handle client
//                    System.out.println("Establish a new connection.\n"
//                            + "Current client: " + Arrays.toString(clients.toArray()));

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

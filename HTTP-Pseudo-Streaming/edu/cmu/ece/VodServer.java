package edu.cmu.ece;

import java.io.*;
import java.net.*;

public class VodServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        int port = Integer.valueOf(args[0]);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started listening on port: " + port + "...");

            // keep listening
            while (true) {
                try {
                    HttpServer server = new HttpServer(serverSocket.accept());
                    // create thread to handle client
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

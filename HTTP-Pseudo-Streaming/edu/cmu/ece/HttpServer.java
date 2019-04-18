package edu.cmu.ece;

import java.net.*;
import java.io.*;
import java.util.*;

public class HttpServer implements Runnable {
    static final File ROOT_DIR = new File("content");
    private Socket clientSocket;
    boolean verbose = true;
    HttpResponse response = new HttpResponse();

    public HttpServer(Socket socket) {
        clientSocket = socket;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter textOut = null;
        BufferedOutputStream binaryOut = null;
        boolean conn = true;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            textOut = new PrintWriter(clientSocket.getOutputStream(), true);
            binaryOut = new BufferedOutputStream(clientSocket.getOutputStream());
            Map<String, String> request = parse(in);

            // check connection status
            if (request.get("connection") != null && request.get("connection").equals("close")) {
                conn = false;
            }

            if (verbose) {
                System.out.println("-------Response--------");
            }
            if (request.get("method").equals("get")) { // GET method
                // obtain URI file
                File file = new File(ROOT_DIR, request.get("uri"));
                if (!file.exists()) { // file not exist
                    System.err.println("File not exist: " + file.getCanonicalPath());
                    String page = response.getPage404();
                    String header = response.getHeader404(conn, page.length());
                    textOut.write(header);
                    textOut.write(page);
                    textOut.flush();
                } else { // file exists
                    int fileLen = (int) file.length();
                    String header;
                    if (request.get("range") == null) {
                        if (verbose) {
                            System.out.println("200 OK.");
                            System.out.println();
                        }
                        header = "HTTP/1.1 200 OK\r\n";
                    } else {
                        header = "HTTP/1.1 206 Partial content\r\n";
                    }
                    header += response.getHeader(
                            conn,
                            fileLen,
                            "",
                            request.get("uri"),
                            file.lastModified());
                    textOut.write(header);
                    textOut.flush();
                    byte[] data = readFile(file, fileLen);
                    binaryOut.write(data, 0, fileLen);
                    binaryOut.flush();
                }

            } else { // not GET method
                throw new IOException("Not Implemented method.");
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            String page = response.getPage500();
            String header = response.getHeader500();
            textOut.write(header);
            textOut.write(page);
            textOut.flush();
        }
    }

    private byte[] readFile(File file, int fileLen) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLen];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
        return fileData;
    }

    public Map<String, String> parse(BufferedReader in) throws IOException {
        Map<String, String> map = new HashMap<>();
        String inputLine = in.readLine(); // first line
        map.put("request", inputLine);
        String[] tokens = inputLine.split(" ");
        if (tokens.length < 3) {
            throw new IOException("Invalid request header. ");
        }
        String method = tokens[0].toLowerCase();
        String uri = tokens[1].toLowerCase();
        if (uri.equals("/")) {
            uri = "index.html";
        }
        map.put("uri", uri);
        map.put("method", method);

        while (!(inputLine = in.readLine()).equals("")) {
            tokens = inputLine.split(":");
            if (tokens.length == 2) {
                map.put(tokens[0].toLowerCase(), tokens[1].trim().toLowerCase());
            }
        }
        if (verbose) {
            System.out.println("-------Request---------");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                System.out.println(entry);
            }
        }
        return map;
    }
}

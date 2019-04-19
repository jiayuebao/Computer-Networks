package edu.cmu.ece;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.net.*;
import java.io.*;
import java.util.*;

public class HttpServer implements Runnable {
    static final File ROOT_DIR = new File("content");
    static final int MAX_BUF_SIZE = 1 << 10;
    private Socket clientSocket;
    boolean verbose = false;
    HttpResponse response = new HttpResponse();
    BufferedReader in = null;
    PrintWriter textOut = null;
    BufferedOutputStream binaryOut = null;
    boolean conn = true;
    int id;
    public HttpServer(Socket socket, int id) {
        clientSocket = socket;
        this.id = id;
    }

    @Override
    public void run() {
        System.out.println("Client id: " + id);
        while (conn) {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                textOut = new PrintWriter(clientSocket.getOutputStream(), true);
                binaryOut = new BufferedOutputStream(clientSocket.getOutputStream());
                String inputLine = null;
                while ( (inputLine = in.readLine())!= null) {
                    doit(inputLine);
                }
            } catch (IOException e) {
                conn = false;
                System.err.println(e.getMessage());
            }
        }
        cleanUp();
    }

    private void doit(String requestLine) {
        try {
            Map<String, String> request = parse(in, requestLine);
            if (verbose) System.out.println("-------Response--------");

            if (!request.get("method").equals("get")) {
                if (verbose) System.out.println("Not GET method!");
                throw new IOException();
            }
            // check connection status
            if (request.get("connection") != null
                    && request.get("connection").equals("close")) {
                conn = false;
            }

            // obtain URI file
            File file = new File(ROOT_DIR, request.get("uri"));
            // 404 file not exist
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
            // file exists
            int fileLen = (int) file.length();
            int rangeLow;
            int rangeHigh;
            String header;
            if (request.get("range") == null) { // 200 OK
                header = "HTTP/1.1 200 OK\r\n";
                rangeLow = 0;
                rangeHigh = fileLen - 1;
            } else { // 206 partial content
                header = "HTTP/1.1 206 Partial content\r\n";
                String range = request.get("range").split("=")[1];
                int d = range.indexOf("-");
                if (d + 1 == range.length()) { // 1-
                    rangeLow = Integer.valueOf(range.substring(0, d));
                    rangeHigh = fileLen - 1;
                } else if (d == 0) { // -500
                    rangeHigh = fileLen - 1;
                    rangeLow = rangeHigh - Integer.valueOf(range.substring(d + 1)) + 1;
                } else { // 1-499
                    rangeLow = Integer.valueOf(range.substring(0, d));
                    rangeHigh = Integer.valueOf(range.substring(d + 1));
                }
            }
            // write header
            int contentLen = rangeHigh - rangeLow + 1;
            String contentRange = rangeLow + "-" + rangeHigh;
            header += response.getHeader(
                    conn,
                    fileLen,
                    contentLen,
                    contentRange,
                    request.get("uri"),
                    file.lastModified());
            textOut.write(header);
            textOut.flush();
            if (verbose) {
                System.out.println(header);
            }

            // write binary file data
            byte[] data = new byte[contentLen];
            FileInputStream fileIn = null;
            try {
                fileIn = new FileInputStream(file);
                fileIn.skip(rangeLow);
                fileIn.read(data);
            } finally {
                if (fileIn != null)
                    fileIn.close();
            }
            binaryOut.write(data, 0, contentLen);
            binaryOut.flush();

        } catch (FileNotFoundException e) {
            String header404 = handle404(conn);
            if (verbose) {
                System.err.println("404 Error File not exist!");
                System.out.println(header404);
            }
        } catch (SocketException e) {
            conn = false;
            System.err.println("Socket error: " + e.getMessage());
        } catch (IOException e) {
            conn = false;
            String header500 = handle500();
            System.err.println("500 Server error: " + e.getMessage());
            if (verbose) {
                System.out.println(header500);
            }
        }
    }
    private int readFile(byte[] fileData, File file) throws IOException {
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(file);
            return fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
    }

    public Map<String, String> parse(BufferedReader in, String requestLine) throws IOException {
        String inputLine = null;
        Map<String, String> map = new HashMap<>();
        map.put("request", requestLine);
        String[] tokens = requestLine.split(" ");
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
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
        return map;
    }

    private String handle404(boolean conn) {
        String page = response.getPage404();
        String header = response.getHeader404(conn, page.length());
        textOut.write(header);
        textOut.write(page);
        textOut.flush();
        return header;
    }

    private String handle500() {
        String page = response.getPage500();
        String header = response.getHeader500();
        textOut.write(header);
        textOut.write(page);
        textOut.flush();
        return header;
    }

    private void cleanUp() {
        try {
            in.close();
            textOut.close();
            binaryOut.close();
            clientSocket.close();
            if (verbose) {
                System.out.println("Close client socket.");
            }
        } catch (IOException e) {
            System.err.println("Close stream error.");
        }
    }
}

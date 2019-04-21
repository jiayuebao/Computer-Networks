package edu.cmu.ece;

import java.net.*;
import java.io.*;
import java.util.*;

public class HttpServer implements Runnable {
    private static final File ROOT_DIR = new File("content");
    private final int id;
    private Socket clientSocket;
    private HttpResponse response = new HttpResponse();
    private BufferedReader in = null;
    private PrintWriter textOut = null;
    private BufferedOutputStream binaryOut = null;
    private boolean conn = true;
    private int transmission = 0; // number of transmission
    private boolean verbose = true;

    public HttpServer(Socket socket, int id) {
        clientSocket = socket;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            textOut = new PrintWriter(clientSocket.getOutputStream(), true);
            binaryOut = new BufferedOutputStream(clientSocket.getOutputStream());
            while (conn) {
                String inputLine = in.readLine();
                if ((inputLine) == null) {
                    conn = false;
                } else {
                    transmission += 1;
                    doit(inputLine);
                }
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        cleanUp(); // close the socket
    }

    private void doit(String requestLine) {
        String verbResponse = String.format("-------[Client %d] Response (%d) --------\n", id, transmission);
        String verbRequest = String.format("-------[Client %d] Request (%d) --------\n", id, transmission);
        if (verbose) {
            System.out.println(String.format("========= Client %d Start (%d) ============", id, transmission));
        }
        try {
            Map<String, String> request = parse(in, requestLine);
            if (verbose) {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<String, String> entry : request.entrySet()) {
                    builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                System.out.println(verbRequest + builder.toString());
            }
            // check method
            if (!request.get("method").equals("get")) {
                if (verbose) {
                    System.out.println(verbResponse + "Not GET method!");
                }
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
                System.out.println(verbResponse + header);
            }

            // write binary file data
            byte[] data = new byte[contentLen];
            readFromFile(data, file, rangeLow);
            binaryOut.write(data, 0, contentLen);
            binaryOut.flush();

        } catch (FileNotFoundException e) {
            String header404 = handle404(conn);
            if (verbose) {
                System.out.println(verbResponse + header404);
            }
        } catch (SocketException e) {
            conn = false;
            System.err.println("[Client " + id + " (" + transmission + ")" + "] Socket error: " + e.getMessage());
        } catch (IOException e) {
            conn = false;
            String header500 = handle500();
            System.err.println("[Client " + id + " (" + transmission + ")" + "] 500 Server error: " + e.getMessage());
            if (verbose) {
                System.out.println(verbResponse + header500);
            }
        } finally {
            if (verbose) {
                System.out.println(String.format("========= Client %d Finish (%d) ============", id, transmission));
            }
        }
    }

    public void readFromFile(byte[] data, File file, int rangeLow) throws IOException {
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(file);
            fileIn.skip(rangeLow);
            fileIn.read(data);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
    }

    public void readFromCache() {

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
//            textOut.println("Connection: Close");
            in.close();
            textOut.close();
            binaryOut.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Close stream error.");
        } finally {
            VodServer.clients.remove(id);
            if (verbose) {
                String str = String.format("========= Client %d Close (%d) ============\n", id, transmission);
                System.out.println(str + "Current client number: " + VodServer.clients.size());
            }
        }
    }
}

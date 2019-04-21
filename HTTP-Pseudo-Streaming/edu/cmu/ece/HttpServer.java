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
    private boolean verbose = true; // request/response information
    private boolean cacheVerbose = false; // cache information
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
                    if (VodServer.mode == 2) {
                        conn = false;
                    }
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

            // start
            File file = null;
            String uri = request.get("uri");
            String range = request.get("range");
            String key = uri + ((range == null) ? "null" : range);
            int fileLen;
            int contentLen;
            long fileLastModified;
            byte[] data;
            String header;

            if (VodServer.mode > 0 && VodServer.cache.get(key) != null) { // being cached
                 WebFile obj = VodServer.cache.get(key);
                 fileLen = obj.length;
                 fileLastModified = obj.lastModified;
                 int[] ranges = getRange(range, fileLen);
                 header = getHeader(range, ranges, fileLen, uri, fileLastModified);
                 data = VodServer.cache.get(key).data;
                 contentLen = data.length;
                 if (cacheVerbose) {
                     System.out.println("Hit key: " + key);
                 }
            } else { // not being cached/not ab test mode
                if (cacheVerbose) {
                    System.out.println("Miss key: " + key);
                }
                file = new File(ROOT_DIR, request.get("uri"));
                // 404 file not exist
                if (!file.exists()) {
                    throw new FileNotFoundException();
                }
                // file exists
                fileLen = (int) file.length();
                fileLastModified = file.lastModified();
                int[] ranges = getRange(range, fileLen);
                header = getHeader(range, ranges, fileLen, uri, fileLastModified);
                contentLen = ranges[1] - ranges[0] + 1;
                data = new byte[contentLen];
                FileInputStream fileIn = null;
                try {
                    fileIn = new FileInputStream(file);
                    fileIn.skip(ranges[0]);
                    fileIn.read(data);
                } finally {
                    if (fileIn != null)
                        fileIn.close();
                }
                if (VodServer.mode > 0 && fileLen < VodServer.MAX_OBJECT_SIZE) {
                    WebFile fileToCache = new WebFile(fileLen, data, fileLastModified);
                    VodServer.cache.put(key, fileToCache);
                    if (cacheVerbose) {
                        System.out.println("Cache the file.");
                    }
                }
            }

            textOut.write(header);
            textOut.flush();
            if (verbose) {
                System.out.println(verbResponse + header);
            } else {
                System.out.println(header);
            }

            // write binary file data
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

    public int[] getRange(String range, int fileLen) {
        int[] ranges = new int[2];
        if (range == null) { // 200 OK
            ranges[1] = fileLen - 1;
        } else { // 206 partial content
            range = range.split("=")[1];
            int d = range.indexOf("-");
            if (d + 1 == range.length()) { // 1-
                ranges[0] = Integer.valueOf(range.substring(0, d));
                ranges[1] = fileLen - 1;
            } else if (d == 0) { // -500
                ranges[1] = fileLen - 1;
                ranges[0] = ranges[1] - Integer.valueOf(range.substring(d + 1)) + 1;
            } else { // 1-499
                ranges[0] = Integer.valueOf(range.substring(0, d));
                ranges[1] = Integer.valueOf(range.substring(d + 1));
            }
        }
        return ranges;
    }

    public String getHeader(String range, int[] ranges, int fileLen, String uri, long fileLastModified) {
        int rangeLow = ranges[0];
        int rangeHigh = ranges[1];
        // write header
        String header;
        int contentLen = rangeHigh - rangeLow + 1;
        String contentRange = rangeLow + "-" + rangeHigh;

        if (range == null) {
            header = "HTTP/1.1 200 OK\r\n";
        } else {
            header = "HTTP/1.1 206 Partial content\r\n";
        }
        header += response.getHeader(
                conn,
                fileLen,
                contentLen,
                contentRange,
                uri,
                fileLastModified);
        return header;
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
//                String str = String.format("========= Client %d Close (%d) ============\n", id, transmission);
//                System.out.println(str + "Current client number: " + VodServer.clients.size());
            }
        }
    }
}
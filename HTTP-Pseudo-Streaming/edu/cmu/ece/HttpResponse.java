package edu.cmu.ece;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class HttpResponse {
    public String getHeader(boolean connect, int fileLen, int contentLen, String contentRange,
                            String uri, long lastModified) {
        StringBuilder builder = new StringBuilder();
        if (VodServer.mode == 1) {
            builder.append("Connection: Keep-Alive\r\n");
        } else {
            builder.append("Connection: ").append(connect ? "Keep-Alive" : "Close").append("\r\n");
        }
        builder.append("Accept-Ranges: bytes\r\n");
        builder.append("Content-Length: ").append(contentLen).append("\r\n");
        builder.append("Content-Range: bytes ").append(contentRange).append("/").append(fileLen).append("\r\n");
        builder.append("Content-Type: ").append(getContentType(uri)).append("\r\n");
        builder.append("Date: ").append(getFormattedDate(new Date())).append("\r\n");
        builder.append("Last-Modified: ").append(getFormattedDate(new Date(lastModified))).append("\r\n");
        builder.append("\r\n");
        return builder.toString();
    }

    public String getHeader404(boolean connect, int fileLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("HTTP/1.1 404 Not Found\r\n");
        if (VodServer.mode == 1) {
            builder.append("Connection: Keep-Alive\r\n");
        } else {
            builder.append("Connection: ").append(connect ? "Keep-Alive" : "Close").append("\r\n");
        }
        builder.append("Date: ").append(getFormattedDate(new Date())).append("\r\n");
        builder.append("Content-Type: text/html\r\n");
        builder.append("Content-Length: ").append(fileLen).append("\r\n");
        builder.append("\r\n");
        return builder.toString();
    }

    public String getPage404() {
        return "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>404 Page</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h3>404 Not Found</h3>\n" +
                "    <p>Oops...Something is wrong!</p>\n" +
                "</body>\n" +
                "</html>";
    }

    public String getHeader500() {
        StringBuilder builder = new StringBuilder();
        builder.append("HTTP/1.1 500 Internal Server Error\r\n");
        if (VodServer.mode == 1) {
            builder.append("Connection: Keep-Alive\r\n");
        } else {
            builder.append("Connection: Close\r\n");
        }
        builder.append("\r\n");
        return builder.toString();
    }

    public String getPage500() {
        return "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>500 Page</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h3>500 Internal Server Error</h3>\n" +
                "    <p>Oops...Something is wrong!</p>\n" +
                "</body>\n" +
                "</html>";
    }

    private String getFormattedDate(Date date) {
        SimpleDateFormat formattedDate = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        formattedDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formattedDate.format(date);
    }

    private String getContentType(String uri) {
        if (uri == null) {
            return "application/octet-stream";
        }
        if (uri.endsWith(".txt")) {
            return "text/plain";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else if (uri.endsWith(".htm") || uri.endsWith(".html")) {
            return "text/html";
        } else if (uri.endsWith(".gif")) {
            return "image/gif";
        } else if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (uri.endsWith(".png")) {
            return "image/png";
        } else if (uri.endsWith(".js")) {
            return "application/javascript";
        } else if (uri.endsWith(".webm")) {
            return "video/webm";
        } else if (uri.endsWith(".mp4")) {
            return "video/mp4";
        } else if (uri.endsWith(".ogg")) {
            return "video/ogg";
        } else {
            return "application/octet-stream";
        }
    }
}


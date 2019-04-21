package edu.cmu.ece;

public class WebFile {
    int length;
    byte[] data;
    long lastModified;
    public WebFile(int length, byte[] data, long lastModified) {
        this.length = length;
        this.data = data;
        this.lastModified = lastModified;
    }
}

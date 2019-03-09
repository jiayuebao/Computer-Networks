import java.util.*;
import org.json.*;

public class Node {
    private String uuid;
    private String name;
    private String host;
    private int backEndPort;
    private int frontEndPort;
    private int peerCount;
    private Map<String, Integer> metrics; // uuid : distance
    private Map<String, Long> heartBeat;
    private Map<String, Node> neighbors; // uuid : node

    public Node() {
        uuid = "not-specified";
        name = "not-specified";
        host = "not-specified";
        backEndPort = 18346;
        frontEndPort = 18345;
        peerCount = 0;
        metrics = new HashMap<>();
        heartBeat = new HashMap<>();
        neighbors = new HashMap<>();
    }

    public void setUuid(String id) {
        this.uuid = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setBackEndPort(int port) {
        this.backEndPort = port;
    }

    public void setFrontEndPort(int port) {
        this.frontEndPort = port;
    }

    public void setPeerCount(int count) {
        this.peerCount = count;
    }

    public void addNeighbors(String uuid, Node node) {
        this.neighbors.put(uuid, node);
    }

    public void addMetrics(String uuid, int distance) {
        this.metrics.put(uuid, distance);
    }

    public void addHeartBeat(String uuid, long time) {
        this.heartBeat.put(uuid, time);
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public int getBackEndPort() {
        return this.backEndPort;
    }

    public int getFrontEndPort() {
        return this.frontEndPort;
    }

    public int getPeerCount() {
        return this.peerCount;
    }

    public Map<String, Integer> getMetrics() {
        return this.metrics;
    }

    public Map<String, Node> getNeighbors() {
        return this.neighbors;
    }

    public Map<String, Long> getHeartBeat() {
        return this.heartBeat;
    }
}


import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Contentserver {
    public Map<String, String> readConf(String fileName) {
        File file = new File(fileName);
        Map<String, String> map = new HashMap<String, String>();
        try {
            Scanner scan = new Scanner(file);
            String line = null;
            while (scan.hasNext()) {
                line = scan.nextLine();
                String[] pair = line.split("=");
                map.put(pair[0].trim(), pair[1].trim());
            }
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public Node createNode(Map<String, String> map) {
        // Initialize Node
        Node node = new Node();
        node.setUuid(map.getOrDefault("uuid", "not-specified"));
        node.setBackEndPort(Integer.valueOf(map.getOrDefault("backend_port", "18346")));
        int peerCount = Integer.valueOf(map.getOrDefault("peer_count", "0"));

        // Initialize neighbors/Metrics/heartbeat
        for (int i = 0; i < peerCount; i++) {
            String[] info = map.get("peer_" + i).split(",");
            Node neighbor = new Node();
            neighbor.setUuid(info[0]);
            neighbor.setHost(info[1]);
            neighbor.setBackEndPort(Integer.valueOf(info[2]));
            node.addNeighbors(info[0],neighbor);
            node.addMetrics(info[0], Integer.valueOf(info[3]));
            node.addHeartBeat(info[0], new Date().getTime());
        }
        return node;
    }


    public void updateConf(String fileName, String uuid) {
        File file = new File(fileName);
        Path path = Paths.get(fileName);
        String newLine = "uuid = " + uuid + "\n";

        try {
            List<String> lines = Files.readAllLines(path);
            Files.write(path, newLine.getBytes());
            for (String line : lines) {
                line = line + "\n";
                Files.write(path, line.getBytes(), StandardOpenOption.APPEND);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
        Contentserver server = new Contentserver();
        Map<String, String> parameters = server.readConf(args[1]);
        Node node = server.createNode(parameters);
        String nodeName = parameters.getOrDefault("name", "not-specified");
        if (node.getUuid().equals("not-specified")) {
            node.setUuid(UUID.randomUUID().toString());
            server.updateConf(args[1], node.getUuid());
        }

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                UdpServer udpServer = new UdpServer(node, nodeName);
                try {
                    udpServer.go();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                UdpClient udpClient = new UdpClient(node.getBackEndPort());
                try {
                    udpClient.go();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.start();
        clientThread.start();
    }
}
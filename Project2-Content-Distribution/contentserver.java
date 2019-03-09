import java.io.*;
import java.nio.file.*;
import java.util.*;

public class contentserver {

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
        node.setName(map.getOrDefault("name", "not-specified"));
        node.setBackEndPort(Integer.valueOf(map.getOrDefault("backend_port", "18346")));
        node.setPeerCount(Integer.valueOf(map.getOrDefault("peer_count", "0")));

        // Initialize neighbors
        for (int i = 0; i < node.getPeerCount(); i++) {
            String[] info = map.get("peer_" + i).split(",");
            Node neighbor = new Node();
            neighbor.setUuid(info[0]);
            neighbor.setHost(info[1]);
            neighbor.setBackEndPort(Integer.valueOf(info[2]));
            node.addNeighbors(info[0],neighbor);
            node.addMetrics(info[0], Integer.valueOf(info[3]));
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
        contentserver server = new contentserver();
        Map<String, String> parameters = server.readConf(args[1]);
        Node node = server.createNode(parameters);

        if (node.getUuid().equals("not-specified")) {
            node.setUuid(UUID.randomUUID().toString());
            server.updateConf(args[1], node.getUuid());
        }

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                UdpServer udpServer = new UdpServer(node);
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
                UdpClient udpClient = new UdpClient(node);
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
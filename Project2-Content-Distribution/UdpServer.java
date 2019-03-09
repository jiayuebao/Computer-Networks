import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
 
public class UdpServer {
	Node node;

	public UdpServer(Node node) {
		this.node = node;
	}

	public String uuidHandler() throws Exception {
		JSONObject obj = new JSONObject();
		obj.put("uuid", node.getUuid());
		return obj.toString();
	}

	public String neighborsHandler() throws Exception {
		JSONArray array = new JSONArray();
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Integer> metric = node.getMetrics();

		for (Node neighbor : neighbors.values()) {
			JSONObject obj = new JSONObject();
			obj.put("uuid", neighbor.getUuid());
			obj.put("name", neighbor.getName());
			obj.put("host", neighbor.getHost());
			obj.put("frontend", neighbor.getFrontEndPort());
			obj.put("backend", neighbor.getBackEndPort());
			obj.put("metric", metric.get(neighbor.getUuid()));
			array.put(obj);
		}
		return array.toString();
	}

	public String addNeighborHandler(String message) {
		System.out.println(message);
		String[] msg = message.split(" ");
		String uuid = msg[1].substring(msg[1].indexOf("=")+1);

		Node neighbor = node.getNeighbors().getOrDefault(uuid, new Node());
		neighbor.setUuid(uuid);
		neighbor.setHost(msg[2].substring(msg[2].indexOf("=")+1));
		neighbor.setFrontEndPort(Integer.valueOf(msg[3].substring(msg[3].indexOf("=")+1)));
		neighbor.setBackEndPort(Integer.valueOf(msg[4].substring(msg[4].indexOf("=")+1)));
		node.addNeighbors(uuid, neighbor);
		node.addMetrics(uuid, Integer.valueOf(msg[5].substring(msg[5].indexOf("=")+1)));

		return "Successfully add Neighbor: " + uuid;
	}


	public String heartBeatHandler(String receiveMessage) throws Exception {
		String[] message = receiveMessage.split(",");
		String uuid = message[1];
		String name = message[2];
		int frontendPort = Integer.valueOf(message[3]);
		long sentTime = Long.valueOf(message[4]);
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Long> heartBeats = node.getHeartBeat();
		if (neighbors.containsKey(uuid)) {
			Node neighbor = neighbors.get(uuid);
			neighbor.setName(name);
			neighbor.setFrontEndPort(frontendPort);
			neighbors.put(uuid, neighbor);
			heartBeats.put(uuid, sentTime);
		}
		return "Heart Beat Sent.";
	}

	public void sendHeartBeat() throws Exception {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				long currTime = new Date().getTime();
				StringBuilder builder = new StringBuilder();
				builder.append("heartbeat,");
				builder.append(node.getUuid() + ",");
				builder.append(node.getName() + ",");
				builder.append(node.getFrontEndPort() + ",");
				builder.append(currTime);
				String message = builder.toString();
				byte[] bytes = message.getBytes();
				Map<String, Node> neighbors = node.getNeighbors();
				Map<String, Long> heartBeat = node.getHeartBeat();
				for (String uuid : neighbors.keySet()) {
					if (currTime < heartBeat.get(uuid)) continue;
					if (currTime - heartBeat.get(uuid) > 1000 * 30) { // not an alive node
						heartBeat.remove(uuid);
						continue;
					}
					// send heartbeat
					try {
						Node neighbor = neighbors.get(uuid);
						DatagramPacket hbpack = new DatagramPacket(bytes,
								bytes.length,
								InetAddress.getByName(neighbor.getHost()),
								neighbor.getBackEndPort());
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 0,1000*10);
	}

	public void go() throws Exception {
		DatagramSocket dsock = new DatagramSocket(node.getBackEndPort());
		DatagramPacket dpack = new DatagramPacket(new byte[150], 150);
		node.setHost(InetAddress.getLocalHost().getHostName());
		sendHeartBeat();

		while(true) {
			// receive the packet
			dsock.receive(dpack);
			String receiveMessage = new String(dpack.getData(), 0, dpack.getLength());
			node.setFrontEndPort(dpack.getPort());
			//System.out.println(new Date( ) + "  " + dpack.getAddress( ) + " : "+ dpack.getPort( ) + " "+ receiveMessage);
			String response = "";

			if (receiveMessage.equals("uuid")) {
				response = uuidHandler();
			} else if (receiveMessage.equals("neighbors")) {
				response = neighborsHandler();
			} else if (receiveMessage.startsWith("addneighbor")) {
				response = addNeighborHandler(receiveMessage);
			} else if (receiveMessage.equals("map")) {

			} else if (receiveMessage.equals("rank")) {

			} else if (receiveMessage.equals("kill")) {
				System.exit(0);
			} else if (receiveMessage.startsWith("heartbeat")) {
				heartBeatHandler(receiveMessage);
			}

			// send the packet
//			byte bytes[] = response.getBytes();
//			dpack = new DatagramPacket(bytes,
//					bytes.length,
//					InetAddress.getByName("localhost"),
//					node.getFrontEndPort());
//			dsock.send(dpack);

			System.out.println(response);
		}
	}
}
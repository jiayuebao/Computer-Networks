import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
 
public class UdpServer {
	Node node;
	Map<String, Advertisement> linkMap; // uuid : advertisement
	Map<String, String> nameTable; // uuid : name
	long sequence = 0;

	public UdpServer(Node node) {
		this.node = node; // init node
		// init linkMap
		linkMap = new HashMap<>();
		Advertisement initAd = new Advertisement(sequence++, node.getMetrics());
		linkMap.put(node.getUuid(), initAd);
		// init nameTable
		nameTable = new HashMap<>();
		nameTable.put(node.getUuid(), node.getName());
		for (String uuid : node.getNeighbors().keySet()) {
			nameTable.put(uuid, node.getNeighbors().get(uuid).getName());
		}
	}

	/**
	 *
	 * @return JSONObject
	 * @throws Exception
	 */
	public String uuidHandler() throws Exception {
		JSONObject obj = new JSONObject();
		obj.put("uuid", node.getUuid());
		return obj.toString();
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public String neighborsHandler() throws Exception {
		JSONArray array = new JSONArray();
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Integer> metric = node.getMetrics();

		for (Node neighbor : neighbors.values()) {
			JSONObject obj = new JSONObject();
			obj.put("uuid", neighbor.getUuid());
			obj.put("name", neighbor.getName());
			obj.put("host", neighbor.getHost());
			obj.put("backend", neighbor.getBackEndPort());
			obj.put("metric", metric.get(neighbor.getUuid()));
			array.put(obj);
		}
		return array.toString();
	}

	/**
	 *
	 * @param message
	 * @return
	 */
	public String addNeighborHandler(String message) {
		String[] fields = new String[] {"uuid=", "host=", "backend=", "metric="};
		int[] startIdx = new int[4];
		int[] endIdx = new int[4];
		for (int i = 0; i < 4; i++) {
			startIdx[i] = message.indexOf(fields[i]) + fields[i].length();
			if (i == 3) endIdx[i] = message.length();
			else endIdx[i] = message.indexOf(fields[i+1]);
		}

		String uuid = message.substring(startIdx[0], endIdx[0]);

		Node neighbor = node.getNeighbors().getOrDefault(uuid, new Node());
		neighbor.setUuid(uuid);
		neighbor.setHost(message.substring(startIdx[1], endIdx[1]));
		neighbor.setBackEndPort(Integer.valueOf(message.substring(startIdx[2], endIdx[2])));
		node.addNeighbors(uuid, neighbor);
		node.addMetrics(uuid, Integer.valueOf(message.substring(startIdx[3], endIdx[3])));
		node.addHeartBeat(uuid, new Date().getTime());

		return "Successfully add Neighbor: " + uuid;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public String mapHandler() throws Exception {
		JSONObject obj = new JSONObject();
		for (String sourceId : linkMap.keySet()) {
			Map<String, Integer> metrics = linkMap.get(sourceId).getMetrics();
			JSONObject obj2 = new JSONObject();
			for (String  neighborId : metrics.keySet()) {
				String name = nameTable.get(neighborId);
				if (name == null || name.equals("not-specified")) {
					name = neighborId;
				}
				obj2.put(name, metrics.get(neighborId));
			}
			String sourceName = nameTable.get(sourceId);
			if (sourceName == null || sourceName.equals("not-specified")) {
				sourceName = sourceId;
			}
			obj.put(sourceName, obj2);
		}
		return obj.toString();
	}

	public String rankHandler() throws Exception {

		JSONArray array = new JSONArray();
		PriorityQueue<GraphNode> pq = new PriorityQueue<>((n1, n2) -> (n1.distance - n2.distance));
		Set<String> visited = new HashSet<>();

		for (String uuid : this.node.getMetrics().keySet()) {
			GraphNode curr = new GraphNode(uuid, this.node.getMetrics().get(uuid));
			pq.offer(curr);
			visited.add(uuid);
		}

		while (!pq.isEmpty()) {
			JSONObject obj = new JSONObject();
			GraphNode top = pq.poll();
			String name = nameTable.get(top.uuid);
			if (name == null || name.equals("not-specified")) {
				name = top.uuid;
			}
			obj.put(name, top.getDistance());
			array.put(obj);
			visited.add(top.getUuid());

			if (linkMap.containsKey(top.getUuid())) {
				Map<String, Integer> metrics = linkMap.get(top.getUuid()).getMetrics();
				for (String uuid : metrics.keySet()) {
					if (!visited.add(uuid)) {
						GraphNode newNeighbor = new GraphNode(uuid,
								top.getDistance() + metrics.get(uuid));
						pq.offer(newNeighbor);
					}
				}
			}
		}

		return array.toString();
	}

	public String heartBeatHandler(String receiveMessage) throws Exception {
		String[] message = receiveMessage.split(",");
		String uuid = message[1];
		String name = message[2];
		long sentTime = Long.valueOf(message[3]);
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Long> heartBeats = node.getHeartBeat();
		if (neighbors.containsKey(uuid)) {
			Node neighbor = neighbors.get(uuid);
			if (neighbor.getName().equals("not-specified")){
				neighbor.setName(name);
			}
			neighbors.put(uuid, neighbor);
			heartBeats.put(uuid, sentTime);
		}
		updateNameTable(uuid, name);
		System.out.println("Received heartbeat from " + uuid + ":" + name + " at " + sentTime);
		return "Received Heart Beat from " + uuid + ":" + name;
	}

	public void sendHeartBeat(DatagramSocket dsock) throws Exception {
		TimerTask heartBeatTask = new TimerTask() {
			@Override
			public void run() {
				long currTime = new Date().getTime();
				StringBuilder builder = new StringBuilder();
				builder.append("heartbeat,");
				builder.append(node.getUuid() + ",");
				builder.append(node.getName() + ",");
				builder.append(currTime);
				String message = builder.toString();
				byte[] bytes = message.getBytes();
				Map<String, Node> neighbors = node.getNeighbors();
				Map<String, Long> heartBeat = node.getHeartBeat();
				Map<String, Integer> metrics = node.getMetrics();
				Set<String> inactive = new HashSet<>();
				for (String uuid : node.getNeighbors().keySet()) {
					if (currTime < heartBeat.get(uuid)) continue;
					if (currTime - heartBeat.get(uuid) > 1000 * 30) { // not an alive node
						inactive.add(uuid);
						continue;
					}
					// send heartbeat
					try {
						Node neighbor = neighbors.get(uuid);
						DatagramPacket hbpack = new DatagramPacket(bytes,
								bytes.length,
								InetAddress.getByName(neighbor.getHost()),
								neighbor.getBackEndPort());
						dsock.send(hbpack);
						System.out.println("Sent heartbeat to " + uuid + " at " + currTime);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				neighbors.keySet().removeAll(inactive);
				metrics.keySet().removeAll(inactive);
				heartBeat.keySet().removeAll(inactive);

				System.out.println("Sent heartbeat at " + currTime);
			}
		};
		Timer timer = new Timer();
		timer.schedule(heartBeatTask, 0,1000*10);
	}

	public void sendAdvertisement(DatagramSocket dsock)  {
		TimerTask advertisementTask = new TimerTask() {
			@Override
			public void run() {
				StringBuilder builder = new StringBuilder();
				builder.append(sequence + ":");
				builder.append(node.getUuid() + "," + node.getName());
				for (String neighborId : node.getNeighbors().keySet()) {
					builder.append(";");
					builder.append(neighborId + ",");
					builder.append(node.getNeighbors().get(neighborId).getName() + ",");
					builder.append(node.getMetrics().get(neighborId));
				}
				sequence++;
				try {
					forward(builder.toString(), dsock);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
		Timer timer = new Timer();
		timer.schedule(advertisementTask, 0,1000*10);
	}

	public String advertisementHandler(String message, DatagramSocket dsock) throws Exception{
		int index1 = message.indexOf(":");
		long sequence = Long.valueOf(message.substring(0, index1));
		message = message.substring(index1+1);
		String[] nodes = message.split(";");
		String[] rootInfo = nodes[0].split(","); // uuid, name
		updateNameTable(rootInfo[0], rootInfo[1]);
		Advertisement prev = linkMap.get(rootInfo[0]);
		if (prev == null || prev.getSequence() < sequence) {
			// update linkMap
			Map<String, Integer> metrics = new HashMap<>();
			for (int i = 1; i < nodes.length; i++) {
				String[] neighborInfo = nodes[i].split(",");
				updateNameTable(neighborInfo[0], neighborInfo[1]);
				metrics.put(neighborInfo[0], Integer.valueOf(neighborInfo[2]));
			}
			Advertisement curr = new Advertisement(sequence, metrics);
			linkMap.put(rootInfo[0], curr);
		}

		// forward the message to all its neighbors
		forward(message, dsock);
		return "Get an advertisement from " + rootInfo[0] + " : " + rootInfo[1];
	}


	public void forward(String message, DatagramSocket dsock) throws Exception{
		byte[] bytes = message.getBytes();
		for (String uuid : node.getNeighbors().keySet()) {
			Node neighbor = node.getNeighbors().get(uuid);
			DatagramPacket packet = new DatagramPacket(bytes,
					bytes.length,
					InetAddress.getByName(neighbor.getHost()),
					neighbor.getBackEndPort());
			dsock.send(packet);
		}
	}

	public void updateNameTable(String uuid, String name) {
		if (!nameTable.containsKey(uuid) || nameTable.get(uuid).equals("not-specified")) {
			nameTable.put(uuid, name);
		}
	}

	public void go() throws Exception {
		DatagramSocket dsock = new DatagramSocket(node.getBackEndPort());
		DatagramPacket dpack = new DatagramPacket(new byte[150], 150);
		node.setHost(InetAddress.getLocalHost().getHostName());
		//sendHeartBeat(dsock);
		//sendAdvertisement(dsock);
		while(true) {
			// receive the packet
			dsock.receive(dpack);
			String receivedMsg = new String(dpack.getData(), 0, dpack.getLength());
			//System.out.println(new Date( ) + "  " + dpack.getAddress( ) + " : "+ dpack.getPort( ) + " "+ receiveMessage);
			String response = "";

			if (receivedMsg.equals("uuid")) {
				response = uuidHandler();
			} else if (receivedMsg.equals("neighbors")) {
				response = neighborsHandler();
			} else if (receivedMsg.startsWith("addneighbor")) {
				response = addNeighborHandler(receivedMsg);
			} else if (receivedMsg.equals("map")) {
				response = mapHandler();
			} else if (receivedMsg.equals("rank")) {
				response = rankHandler();
			} else if (receivedMsg.equals("kill")) {
				System.exit(0);
			} else if (receivedMsg.startsWith("heartbeat")) {
				response = heartBeatHandler(receivedMsg);
			} else if (receivedMsg.startsWith("advertisement")) {
				response = advertisementHandler(receivedMsg, dsock);
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
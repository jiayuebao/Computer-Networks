import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;
 
public class UdpServer {
	Node node;
	Map<String, Advertisement> linkMap; // uuid : advertisement
	Map<String, String> nameTable; // uuid : name
	long sequence = 0;

	public UdpServer(Node node, String nodeName) {
		this.node = node; // init node
		// init linkMap
		linkMap = new ConcurrentHashMap<>();
		Advertisement initAd = new Advertisement(sequence++, node.getMetrics());
		linkMap.put(node.getUuid(), initAd);
		// init nameTable
		nameTable = new ConcurrentHashMap<>();
		nameTable.put(node.getUuid(), nodeName);
		for (String uuid : node.getNeighbors().keySet()) {
			nameTable.put(uuid, "not-specified");
		}
	}

	/**
	 * Handler for "uuid".
	 * @return the uuid of the current node
	 * @throws Exception
	 */
	public String uuidHandler() throws Exception {
		JSONObject obj = new JSONObject();
		obj.put("uuid", node.getUuid());
		return obj.toString();
	}

	/**
	 * Handler for "neighbors".
	 * @return a list of objects representing all active neighbor
	 * @throws Exception
	 */
	public String neighborsHandler() throws Exception {
		JSONArray array = new JSONArray();
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Integer> metric = node.getMetrics();

		for (Node neighbor : neighbors.values()) {
			JSONObject obj = new JSONObject();
			obj.put("uuid", neighbor.getUuid());
			obj.put("name", nameTable.get(neighbor.getUuid()));
			obj.put("host", neighbor.getHost());
			obj.put("backend", neighbor.getBackEndPort());
			obj.put("metric", metric.get(neighbor.getUuid()));
			array.put(obj);
		}
		return array.toString();
	}

	/**
	 * Handler for "addneighbor".
	 * Add the given node with the given uuid, backend port, and distance metric
	 * as new neighbor.
	 * @param message
	 * @return  success information
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
		updateNameTable(uuid, "not-specified");
		return "Successfully add Neighbor: " + uuid;
	}

	/**
	 * Handler for "map".
	 * Output the latest network map only containing active node.
	 * The object's field name is node's name (by default) or UUID (if a node name was not specified.)
	 * @return an object representing an adjacency list for the latest network map
	 * @throws Exception
	 */
	public String mapHandler() throws Exception {
		JSONObject obj = new JSONObject();
		for (String sourceId : linkMap.keySet()) {
			Map<String, Integer> metrics = linkMap.get(sourceId).getMetrics();
			JSONObject obj2 = new JSONObject();
			for (String  neighborId : metrics.keySet()) {
				String name = getName(neighborId);
				obj2.put(name, metrics.get(neighborId));
			}
			String sourceName = getName(sourceId);
			obj.put(sourceName, obj2);
		}
		return obj.toString();
	}

	/**
	 * Handler for "rank".
	 * @return an ordered list (sorted by distance metrics) showing the distance between the requested nodes.
	 * @throws Exception
	 */
	public String rankHandler() throws Exception {
		JSONArray array = new JSONArray();

		PriorityQueue<GraphNode> pq = new PriorityQueue<>((n1, n2)
				-> (Integer.compare(n1.getDistance(), n2.getDistance())));
		Set<String> visited = new HashSet<>();
		visited.add(node.getUuid());
		for (String uuid : this.node.getMetrics().keySet()) {
			GraphNode curr = new GraphNode(uuid, this.node.getMetrics().get(uuid));
			pq.offer(curr);
		}

		while (!pq.isEmpty()) {
			JSONObject obj = new JSONObject();
			GraphNode top = pq.poll();
			if (visited.contains(top.getUuid())) continue;
			visited.add(top.getUuid());
			obj.put(getName(top.getUuid()), top.getDistance());
			array.put(obj);

			if (linkMap.containsKey(top.getUuid())) {
				Map<String, Integer> metrics = linkMap.get(top.getUuid()).getMetrics();
				for (String uuid : metrics.keySet()) {
					if (!visited.contains(uuid)) {
						GraphNode newNeighbor = new GraphNode(uuid,
								top.getDistance() + metrics.get(uuid));
						pq.offer(newNeighbor);
					}
				}
			}
		}

		return array.toString();
	}

	/**
	 * Handler for heartbeat.
	 * Receive the heartbeat information from neighbors.
	 * @param receiveMessage
	 * @return success information
	 * @throws Exception
	 */
	public String heartBeatHandler(String receiveMessage,DatagramPacket dpack) throws Exception {
		String[] message = receiveMessage.split(",");
		String uuid = message[1];
		String name = message[2];
		long sentTime = Long.valueOf(message[3]);
		Map<String, Node> neighbors = node.getNeighbors();
		Map<String, Long> heartBeats = node.getHeartBeat();
		Map<String, Integer> metrics = node.getMetrics();

		if (heartBeats.containsKey(uuid)) {
			if (heartBeats.get(uuid) < sentTime) {
				heartBeats.put(uuid, sentTime);
			}
		} else { // a new neighbor
			int distance = Integer.valueOf(message[4]);
			Node neighbor = new Node();
			neighbor.setUuid(uuid);
			neighbor.setBackEndPort(dpack.getPort());
			neighbor.setHost(dpack.getAddress().getHostName());
			neighbors.put(uuid, neighbor);
			metrics.put(uuid, distance);
			heartBeats.put(uuid, sentTime);
		}

		updateNameTable(uuid, name);
		return "Received Heart Beat from " + name + " at " + sentTime;
	}

	/**
	 * Send heartbeat information to its neighbors per 10 seconds.
	 * At the same time check if a neighbor is inactive (not send heartbeat for more than 30 seconds),
	 * if not, remove it.
	 * Heart beat information format is:
	 * "heartbeat, rootUuid, rootName, currentTime, distance"
	 * @param dsock
	 * @throws Exception
	 */
	public void sendHeartBeat(DatagramSocket dsock) throws Exception {
		TimerTask heartBeatTask = new TimerTask() {
			@Override
			public void run() {
				long currTime = new Date().getTime();
				StringBuilder builder = new StringBuilder();
				builder.append("heartbeat,");
				builder.append(node.getUuid() + ",");
				builder.append(nameTable.get(node.getUuid()) + ",");
				builder.append(currTime + ",");

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
						StringBuilder builder2 = new StringBuilder(builder);
						builder2.append(node.getMetrics().get(uuid));
						String message = builder2.toString();
						byte[] bytes = message.getBytes();
						Node neighbor = neighbors.get(uuid);
						DatagramPacket hbpack = new DatagramPacket(bytes,
								bytes.length,
								InetAddress.getByName(neighbor.getHost()),
								neighbor.getBackEndPort());
						dsock.send(hbpack);
						//System.out.println("Sent heartbeat to " + nameTable.get(uuid) + " at " + currTime);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				// remove the inactive
				neighbors.keySet().removeAll(inactive);
				metrics.keySet().removeAll(inactive);
				heartBeat.keySet().removeAll(inactive);
				//nameTable.keySet().removeAll(inactive);
			}
		};
		Timer timer = new Timer();
		timer.schedule(heartBeatTask, 0,1000*10);
	}


	/**
	 * Periodically send advertisement (neighbor metric) to the neighbors.
	 * The advertisement format is:
	 * "advertisementsequence : rootUuid, rootName; neighbor1Uuid, neighbor1Name, neighbor1Dist;..."
	 * @param dsock
	 */
	public void sendAdvertisement(DatagramSocket dsock)  {
		TimerTask advertisementTask = new TimerTask() {
			@Override
			public void run() {
				StringBuilder builder = new StringBuilder();
				builder.append("advertisement");
				builder.append(sequence + ":");
				builder.append(node.getUuid() + "," + nameTable.get(node.getUuid()));
				for (String neighborId : node.getNeighbors().keySet()) {
					builder.append(";");
					builder.append(neighborId + ",");
					builder.append(nameTable.get(neighborId) + ",");
					builder.append(node.getMetrics().get(neighborId));
					//System.out.println("Sent advertisement" + sequence + " to " + neighborId);
				}
				sequence++;
				try {
					forward(builder.toString(), dsock, node.getUuid());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
		Timer timer = new Timer();
		timer.schedule(advertisementTask, 0,1000*10);
	}

	/**
	 * Receive the advertisement from the other nodes.
	 * Then forward the message to its neighbors.
	 * @param message
	 * @param dsock
	 * @return success information
	 * @throws Exception
	 */
	public String advertisementHandler(String message, DatagramSocket dsock) throws Exception {
		String origin = message;
		// decode the heartbeat message
		message = message.substring("advertisement".length());
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
				String[] neighborInfo = nodes[i].split(","); // uuid, name, distance
				updateNameTable(neighborInfo[0], neighborInfo[1]);
				metrics.put(neighborInfo[0], Integer.valueOf(neighborInfo[2]));
			}
			Advertisement curr = new Advertisement(sequence, metrics);
			linkMap.put(rootInfo[0], curr);
			// forward the message to all its neighbors
			forward(origin, dsock, rootInfo[0]);
		}

		return "Get an advertisement from " + rootInfo[1];
	}


	/**
	 * Forward the message to all the neighbor node except the excluded neighbor.
	 * @param message
	 * @param dsock
	 * @throws Exception
	 */
	public void forward(String message, DatagramSocket dsock, String excludeId) throws Exception{
		byte[] bytes = message.getBytes();
		for (String uuid : node.getNeighbors().keySet()) {
			if (uuid.equals(excludeId)) continue;
			Node neighbor = node.getNeighbors().get(uuid);
			DatagramPacket packet = new DatagramPacket(bytes,
					bytes.length,
					InetAddress.getByName(neighbor.getHost()),
					neighbor.getBackEndPort());
			dsock.send(packet);
		}
	}

	/**
	 * Update the name table if the uuid is not exist or the name is not specified.
	 * @param uuid
	 * @param name
	 */
	public void updateNameTable(String uuid, String name) {
		if (!nameTable.containsKey(uuid) || !name.equals("not-specified")) {
			nameTable.put(uuid, name);
		}
	}

	/**
	 * Check the nameTable to get the node name according to uuid.
	 * @param uuid
	 * @return node name
	 */
	public String getName(String uuid) {
		String name = nameTable.get(uuid);
		if (name == null || name.equals("not-specified")) {
			name = uuid;
		}
		return name;
	}

	/**
	 * Receive the request from the console and use corresponding to handle the request.
	 * Print the response on the screen.
	 * @throws Exception
	 */
	public void go() throws Exception {
		DatagramSocket dsock = new DatagramSocket(node.getBackEndPort());
		DatagramPacket dpack = new DatagramPacket(new byte[1500], 1500);
		node.setHost(InetAddress.getLocalHost().getHostName());
		sendHeartBeat(dsock);
		sendAdvertisement(dsock);
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
				heartBeatHandler(receivedMsg, dpack);
			} else if (receivedMsg.startsWith("advertisement")) {
				advertisementHandler(receivedMsg, dsock);
			}

			// send the packet
//			byte bytes[] = response.getBytes();
//			dpack = new DatagramPacket(bytes,
//					bytes.length,
//					InetAddress.getByName("localhost"),
//					node.getFrontEndPort());
//			dsock.send(dpack);
			if (!response.equals("")) System.out.println(response);
		}
	}
}
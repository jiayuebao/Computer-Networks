# Computer-Networks-Projects
This is the repository for CMU 18741 Computer Networks projects. 

## A. Content Delivery Network
Content in the Internet is not exclusive to one computer. In this network, a simple link-state routing protocol is implemented. The obtained distance metric can be used to improve transport efficiency.

The program of "Contentserver" will be executed simultaneously on multiple computers. At first, the program takes as input a configuration file that specifies a **uuid**, a **name**, a **port number**, a **hostname** and the **distance metric** of all “neighbors” of any given node (i.e., a computer). Each program will then infer the graph of the entire network in real-time, including the number and list of all nodes, the aggregate distance metric of all nodes reachable from a given node. This program can also quickly respond to newly added nodes and as well as inactive nodes that are taken out of the network.

### How to execute
Put the node configuration file under the same directory of contentserver.sh, and simply run: 

  ```
  sh contentserver.sh -c <your node.conf file>
  ```

### Supporting URI
- **uuid**: the uuid of the current node
- **neighbors**: a list of objects representing all active neighbors
- **addneighbor**: a list of objects representing all active neighbors
- **map**: an object representing an adjacency list for the latest network map. It should contain only active node/link.
- **rank**: an object representing an adjacency list for the latest network map. It should contain only active node/link.
- **kill**: terminate the program

### Design and Implementation 

#### Node
Node class is created to represent a node (i.e., a computer) in the network. A Node is initialized in Contentserver.java and is maintained in UdpServer.java.Each Node has uuid, host and backEndPort and three tables to record its neighbors’ information:
- metrics: record the neighbor distance
- heartBeats: record the last time receiving the neighbor heart beat
- neighbors: record all the neighbor node

#### Contentserver
The main method entry, dealing with node initialization and creating 2 threads to deal with UDP requests and UDP responses. 

#### UDPClient
Waiting for a console input. When there is one, it will create a UDP socket and sends the message in a UDP packet to the UDPserver.

#### UDPServer
Running on a certain port and waiting for a UDP request. When there is one, it calls the corresponding handlers to deal with it. The request may from the frontend or from other nodes sending heartbeat or advertisement. 
The server also creates two threads to periodically send heartbeat and advertisement to its neighbors. Most work is done here:
##### a.	Neighbors Updating
Heart beat is used to know whether the neighbors are keepalive and providing more neighbor information, such as neighbor name (which does not provide in the initialization configuration file), and distance information
##### b.  Network topology map
Advertisement mechanism is used to distribute the network topology and connectivity metrics to every node in the network. A node will periodically send an advertisement to its neighbors. The advertisement includes the current node information and its neighbor metrics. There is also a sequence number attached to each advertisement. If a node receives an advertisement from other node, it will first check the sequence number to see whether the advertisement is the latest one. If it is, then update the network topology map. After that, it will forward the message to all its neighbors except the neighbor who sent the advertisement (if the advertisement is sent from one of its neighbors).
##### c.	Routing table
The routing table is created by finding the shortest path of each node to the current node and ranking them according to the distance. It can be implemented based on the network topology map and Dijkstra's Algorithm. 

### Test Results
1.	There is only one node1 and node2 in the network, so node3(previous neighbor of node1) will disappear after a while.
<img src="https://github.com/jiayuebao/Computer-Networks/blob/master/Project2-Content-Distribution/pictures/running1.png" width=70%, height=70%>

2.	Node1 added node4 as a new neighbor.
<img src="https://github.com/jiayuebao/Computer-Networks/blob/master/Project2-Content-Distribution/pictures/running-node1.png" width=70%, height=70%>

3.	Node4 previously does not have any neighbors, but after a while, it will recognize node1 as its neighbor. Furthermore, it will also realize the existence of node2. And so does node2.
<img src="https://github.com/jiayuebao/Computer-Networks/blob/master/Project2-Content-Distribution/pictures/running-node2.png" width=70%, height=70%>
<img src="https://github.com/jiayuebao/Computer-Networks/blob/master/Project2-Content-Distribution/pictures/running-node4.png" width=70%, height=70%>

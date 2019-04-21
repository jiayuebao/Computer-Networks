# Computer-Networks-Projects
This is the repository for CMU 18741 Computer Networks projects. 

## A. Content Distribution Network
Content in the Internet is not exclusive to one computer. In this network, a simple link-state routing protocol is implemented. The obtained distance metric can be used to improve transport efficiency.

The program of "Contentserver" will be executed simultaneously on multiple computers. At first, the program takes as input a configuration file that specifies a **uuid**, a **name**, a **port number**, a **hostname** and the **distance metric** of all “neighbors” of any given node (i.e., a computer). Each program will then infer the graph of the entire network in real-time, including the number and list of all nodes, the aggregate distance metric of all nodes reachable from a given node. This program can also quickly respond to newly added nodes and as well as inactive nodes that are taken out of the network.

## B. HTTP Psedo Streaming
A web server that supports playing video/image/txt/html/js files. Utilized LRU cache to improve the performance to support 5000 requests (for 100KB content) with 1000 concurrency level in only 500ms serve time (95%). 



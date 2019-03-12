## Content Distribution Network
### How to execute
Put the node configuration file under the same directory of contentserver.sh, and simply run: 

  ```
  sh contentserver.sh -c <your node.conf file>
  ```
e.g.
  ```
  sh contentserver.sh -c node1.conf
  ```
 
To observe how multiple nodes communicate with each other:
1. test on your local machine
```
step1: modify the .conf files to give different nodes different ports and the hostname remains "localhost"
step2: open different terminals and feed different inputs to the program
```
2. test on multiple machines
```
step1: modify the .conf files to set the appropriate hostname for different nodes
step2: run the program with different inputs on different machines
```

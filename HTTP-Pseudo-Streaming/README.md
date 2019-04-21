## HTTP Pseudo Streaming
### How to execute
simply run: 

  ```
  ant
  java cmu.edu.ece.VodServer 8080
  ```
 
 Three modes to choose:
 -    mode 0 is suitable for every normal test but not for performance test because it does not include cache. For performance test, please choose mode 1 or mode 2. 
 -    If you choose mode 1, please use the command like:
 `$ ab -k -n 5000 -c 1000 172.19.137.178:8086/sample1.ogg`
 -    If you choose mode 2, please use the command like:
 `$ ab -n 5000 -c 1000 172.19.137.178:8086/sample1.ogg`

 


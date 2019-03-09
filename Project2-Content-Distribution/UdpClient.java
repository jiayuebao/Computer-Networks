import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
  
public class UdpClient {
    Node node;
    public UdpClient(Node node) {
        this.node = node;
    }
    public void go() throws IOException{
        DatagramSocket dsock = new DatagramSocket();
        while (true) {
            // input from the console
            Scanner scan = new Scanner(System.in);
            String request = scan.nextLine();

            // send the packet
            byte[] bytes = request.getBytes();
            DatagramPacket dpack = new DatagramPacket(bytes,
                    bytes.length,
                    InetAddress.getByName("localhost"),
                    node.getBackEndPort());
            dsock.send(dpack);

            // receive the packet
//            dpack = new DatagramPacket(new byte[150], 150);
//            dsock.receive(dpack);
//            String response = new String(dpack.getData(), 0, dpack.getLength());
//            System.out.println("response from client: " + response);
        }
    }
}
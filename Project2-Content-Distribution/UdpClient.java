import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
  
public class UdpClient {
    int backEndport;
    public UdpClient(int port) {
        this.backEndport = port;
    }
    public void go() throws IOException{
        while (true) {
            DatagramSocket dsock = new DatagramSocket();
            // input from the console
            Scanner scan = new Scanner(System.in);
            String request = scan.nextLine();

            // send the packet
            byte[] bytes = request.getBytes();
            DatagramPacket dpack = new DatagramPacket(bytes,
                    bytes.length,
                    InetAddress.getByName("localhost"),
                    this.backEndport);
            dsock.send(dpack);

            // receive the packet
//            dpack = new DatagramPacket(new byte[150], 150);
//            dsock.receive(dpack);
//            String response = new String(dpack.getData(), 0, dpack.getLength());
//            System.out.println("response from client: " + response);
        }
    }
}
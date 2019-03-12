import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;

public class Helper {
    class Node {
        Map<Integer, Integer> map = new HashMap<>();

    }
    public static void main(String[] args) {
        Helper helper = new Helper();
        helper.heartBeat();
    }

    public void heartBeat() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                long currTime = new Date().getTime();
                System.out.println("Sent heartbeat at " + currTime);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0,1000*10);
    }
}

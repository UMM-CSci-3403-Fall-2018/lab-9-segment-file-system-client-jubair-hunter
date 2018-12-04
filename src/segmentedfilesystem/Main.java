package segmentedfilesystem;

public class Main {

    public static void main(String[] args) {

        //Declare local variables
        int port;
        InetAddress address = "heartofgold.morris.umn.edu";
        DatagramSocket socket = 6014;
        DatagramPacket packet;
        byte[] sendBuf = new byte[1028];


        // Process the command-line args used to invoke the method
        if (args.length != 1) {
           System.out.println("Usage: java QuoteClient heartofgold.morris.umn.edu");
            return;
          }

        //QuoteClient sends a request to the server
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = new byte[1028];
        InetAddress address = InetAddress.getByName(heartofgold.morris.umn.edu);
        DatagramPacket packet = new DatagramPacket(buf, vuf.length, adress, 6014);
        socket.send(packet);

        //Client gets a response from the server and displays it
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
          }

}

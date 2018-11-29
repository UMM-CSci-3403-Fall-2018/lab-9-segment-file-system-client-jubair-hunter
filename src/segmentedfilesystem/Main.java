package segmentedfilesystem;

public class Main {

    public static void main(String[] args) {

        //Declare local variables
        int port;
        InetAddress address = null;
        DatagramSocket socket = null;
        DatagramPacket packet;
        byte[] sendBuf = new byte[256];


        // Process the command-line args used to invoke the method
        if (args.length != 1) {
           System.out.println("Usage: java QuoteClient <hostname>");
            return;
          }

        //QuoteClient sends a request to the server
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = new byte[256];
        InetAddress address = InetAddress.getByName(args[0]);
        DatagramPacket packet = new DatagramPacket(buf, vuf.length, adress, 4445);
        socket.send(packet);

        //Client gets a response from the server and displays it
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received - new String(packet.getData(), 0, packet.getLength());
    }

}

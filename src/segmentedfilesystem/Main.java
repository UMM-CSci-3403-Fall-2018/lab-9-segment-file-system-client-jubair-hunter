package segmentedfilesystem;

public class Main {
    
    public static void main(String[] args) {
        
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
        
        
        
    }

}

/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.net;

import java.util.*;
import java.io.*;
import java.nio.*;

/**
 * A test program that acts as a client to interface with a CTP server.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class CTPClient {

    /** the connection to act as a client on */
    private CTPConnection connection = null;
    
    /**
     * Creates a new CTPConnectionManager and possibly a connection.
     */
    public void start(int listenPort, String targetHost, int targetPort) throws IOException {
        
        // start the connection manager
        CTPConnectionManager manager = new CTPConnectionManager(new ClientHandlerFactory(), listenPort);
        manager.start();
        
        // connect to the target host
        if(targetHost != null) {
            manager.connect(targetHost, targetPort, new ClientHandler());
        
            // wait for the connection
            while(true) {
                synchronized(this) {
                    if(connection != null) break;
                }
            }
            
            // read data and write it to the connection
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                String dataString = in.readLine();
                synchronized(this) {
                    if(connection == null) break;
                }

                if(dataString != null) {
                    System.out.println("read a string of length " + dataString.length());
                    ByteBuffer data = ByteBuffer.wrap(dataString.getBytes("US-ASCII"));
                    connection.sendChunk(data);
                } else {
                    connection.close();
                }
            }
        }
    }
    
    /**
     * Creates a new CTPClient and starts it.
     */
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: CTPClient <listenport> [<targethost> <targetport>]");
            return;
        }
        
        // parse input
        int listenPort = Integer.parseInt(args[0]);
        String targetHost = null;
        int targetPort = -1;
        if(args.length == 3) {
            targetHost = args[1];
            targetPort = Integer.parseInt(args[2]);
        }
        
        // start it up
        try {
            new CTPClient().start(listenPort, targetHost, targetPort);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Simple handlers display user text as typed.
     */
    class ClientHandler implements CTPHandler {
        public void connectionClosed(CTPConnection source, Exception reason) {
            if(reason == null) System.out.println("CLOSED: " + source);
            else System.out.println("CLOSED " + source + ", REASON=" + reason.getMessage());
            synchronized(CTPClient.this) {
                connection = null;
            }
        }
        public void connectionReady(CTPConnection source) {
            System.out.println("READY: " + source);
            synchronized(CTPClient.this) {
                connection = source;
            }
        }
        public void receiveChunk(CTPConnection source, ByteBuffer data) {
            try {
                byte[] dataBytes = new byte[data.remaining()];
                data.get(dataBytes);
                String dataString = new String(dataBytes, "US-ASCII");
                System.out.println("DATA: \"" + dataString + "\"");
            } catch(UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    class ClientHandlerFactory implements CTPHandlerFactory {
        public CTPHandler constructHandler() {
            return new ClientHandler();
        }
    }
}

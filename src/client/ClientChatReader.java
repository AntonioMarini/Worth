package client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

// classe che sta in ascolto su una multicast socket udp per ricevere messaggi 
// mettendoli nella lista messages condivisa con un ClientChat
public class ClientChatReader implements Runnable{

    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private static final int MAX_LEN = 1000;
    private ArrayList<String> messages;
    private boolean done;

    public ClientChatReader(ArrayList<String> messages, MulticastSocket socket, InetAddress group, int port)
    {
        this.socket = socket;
        this.group = group;
        this.port = port;
        this.messages = messages;

        this.done = false;
    }

    @Override
    public void run() {
        byte[] buf = new byte[8192];
        ByteArrayInputStream bin = new ByteArrayInputStream(buf);
        while(!done)
        {
            bin.reset();
            String rMessage;

            try
            {
                DatagramPacket rp = new DatagramPacket(buf,buf.length,group,port);
                socket.receive(rp); // thread si blocca qui
                rMessage = new String(rp.getData(), rp.getOffset(), rp.getLength());
                messages.add(rMessage);
            }
            catch(IOException e)
            {
                stop();
            }
        }
    }

    public synchronized void setMessages(ArrayList<String> messages) {
        this.messages = messages;
    }

    public synchronized void stop(){
        done = true;
    }

}

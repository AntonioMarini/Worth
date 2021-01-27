package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;


/* 
//
// Classe che rappresenta una chat di un progetto.
// @author Antonio Marini
*/
public class Chat {

    private InetAddress group;
    private String address;
    private int port;
    private MulticastSocket mSocket;
    private ArrayList<String> messages;

    private static final  int nMaxMessages = 20; // massimo numero di messaggi che vengono conservati

    public Chat(String address, int port){

        messages = new ArrayList<>(); // chat non persistente nel sistema
        this.address = address;
        this.port = port;

        try{
        group = InetAddress.getByName(address);
        this.mSocket = new MulticastSocket(port);
        mSocket.setTimeToLive(0); // TTL in ambiente localhost (debug)
        mSocket.joinGroup(group);

        }catch(IOException e){}

    }

    //
    public synchronized void writeMessage(String text){

        String message = "server: " + text;
        messages.add(message); // ogni messaggio scritto viene subito aggiunto alla chat

        try
        {
            byte[] buf = message.getBytes();
            DatagramPacket dp = new DatagramPacket(buf, buf.length, group, port);
            mSocket.send(dp);
        }catch (IOException e){e.printStackTrace();}
    }

    // lascia il gruppo e chiude la socket
    public synchronized void closeChat(){
        try{ mSocket.leaveGroup(group);}
        catch (IOException e){e.printStackTrace();}
        mSocket.close();
    }

}

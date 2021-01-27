package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

// Classe che rappresenta una chat di progetto -> invoca un thread readerChat
public class ClientChat {

    private InetAddress group;
    private String address;
    private int port;
    private MulticastSocket mSocket;
    private ArrayList<String> messages;
    private ClientChatReader chatReader;

    @SuppressWarnings("deprecation")
    public ClientChat(String address, int port){

        messages = new ArrayList<>(); // chat non persistente nel sistema
        this.address = address;
        this.port = port;

        try{
        group = InetAddress.getByName(address);
        mSocket = new MulticastSocket(port);
        mSocket.setTimeToLive(0); // TTL in ambiente localhost (debug)
        mSocket.joinGroup(group);

        chatReader = new ClientChatReader(messages, mSocket, group, port);
        Thread readerThread = new Thread(chatReader);
        readerThread.start();

        }catch(IOException e){}


    }

    public void writeMessage(String mittente,String text){

        String message = mittente + ": " + text;

        try
        {
            byte[] buf = message.getBytes();
            DatagramPacket dp = new DatagramPacket(buf, buf.length, group, port);
            mSocket.send(dp);
        }catch (IOException e){e.printStackTrace();}
    }

    //get the last messages and remove them
    public synchronized ArrayList<String> getClientMessages(){

        ArrayList<String> tmp =  messages;

        messages = new ArrayList<String>();
        chatReader.setMessages(messages);


        return tmp;
    }

    @SuppressWarnings("deprecation")
    public void closeChat(){

        try{ mSocket.leaveGroup(group);}
        catch (IOException e){e.printStackTrace();}

        mSocket.close();

        chatReader.stop();
    }

}

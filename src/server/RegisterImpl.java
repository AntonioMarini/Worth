package server;

import client.NotifyInterface;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RegisterImpl extends RemoteServer implements RegisterInterface , Serializable {


    static final long serialVersionUID = -8241807076457781194L;
    private UsersData uData;
    private List<NotifyInterface> clients;

    public RegisterImpl(UsersData uData){
        super();
        this.uData = uData;
        clients = new ArrayList<>();
    }

    @Override
    public String register(String username, String password) throws RemoteException {

        String reply  = "< ";

        User user = new User(username, password);

        if(uData.contains(user))
        {
            reply += "user " + username + " gia registrato.";
            return reply;
        }

        uData.addUser(user);
        reply += username + " registrato su worth.";

        doRegistersCallbacks(username);

        return reply;
    }

    @Override
    public String unregister(String username, String password) throws RemoteException {

        String reply  = "< ";

        if(uData.get(username) == null)
        {
            reply = "user non esiste";
            return reply;
        }

        uData.removeUser(uData.get(username));
        
        reply = "user rimosso correttamente";
        return reply;
    }


    @Override
    public void registerForCallback(NotifyInterface clientInterface) throws RemoteException {
        if(!clients.contains(clientInterface)){
            clients.add(clientInterface);
        }
    }

    @Override
    public void unregisterForCallback(NotifyInterface clientInterface) throws RemoteException {
        if(clients.contains(clientInterface))
            clients.remove(clientInterface);
    }

    public synchronized void updateState(String username, Boolean state) throws RemoteException{
        doStateCallbacks(username, state);
    }

    // callback per mandare stati a tutti i client registrati
    public void doStateCallbacks(String username, Boolean state )throws RemoteException{
        Iterator<NotifyInterface> i = clients.iterator();
        while(i.hasNext()){
            NotifyInterface client = (NotifyInterface) i.next();
            client.notifyState(username, state);
        }
    }


    // callback per mandare stati a tutti i client registrati
    public void doRegistersCallbacks(String username)throws RemoteException{
        Iterator<NotifyInterface> i = clients.iterator();
        while(i.hasNext()){
            NotifyInterface client = (NotifyInterface) i.next();
        client.notifyRegister(username);
    }
}

}

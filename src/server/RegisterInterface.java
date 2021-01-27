package server;



import client.NotifyInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RegisterInterface extends Remote{

    public String register(String username, String password) throws RemoteException;

    public String unregister(String username, String password) throws  RemoteException;

    public void registerForCallback(NotifyInterface clientInterface) throws RemoteException;

    public void unregisterForCallback(NotifyInterface clientInterface) throws  RemoteException;

}

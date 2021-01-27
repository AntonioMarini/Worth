package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyInterface extends Remote {

    //metodo esportato dal client per ricevere notifiche sugli stati degli utenti
    void notifyState(String username, boolean state) throws RemoteException;

    // metodo esportato per ricevere notifiche quando un utenve viene registrato
    void notifyRegister(String username) throws RemoteException;

}

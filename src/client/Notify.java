package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// implementazione dei metodi callback del client (rmi)
public class Notify extends RemoteObject implements NotifyInterface{

    private UsersStates usersStates;

    public Notify(UsersStates userStates) throws RemoteException{
        super();
        this.usersStates = userStates;
    }

    @Override
    public void notifyState(String username, boolean state) throws RemoteException {
        usersStates.setState(username, state); // aggiorna la struttura dati degli stati
        usersStates.printState(username); // stampa il nuovo stato.
    }

    @Override
    public void notifyRegister(String username) throws RemoteException {
        usersStates.getStates().remove(username);
        System.out.println("L'utente " + username + " è stato aggiunto a worth.");
    }
}

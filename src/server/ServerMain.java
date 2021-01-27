package server;


import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// TODO exit safe dal server.

public class ServerMain {

    // voglio che sia globale nel server

    public static UsersData uData;
    public static ProjectData pData;

    public static final int nMAX_THREADS = 30;

    public static void main(String[] args) throws IOException {

        // inizializzo le due strutture dati principali
        uData = new UsersData();
        pData = new ProjectData(uData);

        int registryPort = 9999;
        int tcpConnPort = 6788;

        RegisterImpl reg;

        try{

            // creo istanza oggetto remoto e lo esporto nel registry
            reg = new RegisterImpl(uData);
            stubExport(reg, registryPort);

            // creo un pool per la gestione delle connessioni coi client (massimo 30)
            ExecutorService pool = Executors.newFixedThreadPool(nMAX_THREADS);

            // LOGIN SERVER
            try(ServerSocket listeningSock = new ServerSocket(tcpConnPort);) {
                System.out.println("Server ready.");
                boolean done = false;

                while (!done) {
                    // mi blocco in attesa che un nuovo client apra una connessione
                    pool.execute(new Service(uData, pData, listeningSock.accept(), reg));
                }

                pool.shutdownNow();
            }catch(IOException e){System.err.println("errore I/O nella serversocket.");}
        }
        catch(RemoteException rex){rex.printStackTrace(); }
    }

    public static void stubExport(RegisterImpl reg, int registryPort) throws RemoteException {
        // creazione ed esportazione dello stub del server
        RegisterInterface stub = (RegisterInterface) UnicastRemoteObject.exportObject(reg,0); // port = 0 -> richiede porta anonima
        LocateRegistry.createRegistry(registryPort); // creazione di un registry sulla porta port.
        Registry r = LocateRegistry.getRegistry(registryPort); //prendiamo il riferimento del registry
        r.rebind("WORTH-SERVER", stub); //pubblico lo stub nel registry
    }

}

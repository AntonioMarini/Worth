package client;


import server.RegisterInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;


// Classe che contiene il main del client
//
// simula una console su cui far scrivere un utente i comandi
//
// @author Antonio Marini
public class ClientMain {

    public static ArrayList<String> main_menu = new ArrayList<>();
    public static Scanner scanner = new Scanner(System.in);
    private static String userLogged;

    // struttura dati che mantiene le chat dei progetti dell'utente loggato
    private static HashMap<String, ClientChat>  projChats; // key = indirizzo della chat
    private static ProjectsInfo projInfo;

    private static RegisterInterface serverStub;
    private static NotifyInterface clientStub;

    private static UsersStates uStates;

    public static void main(String[] args) throws IOException, ClassNotFoundException {


        uStates = new UsersStates();

        getROS(); // si ottiene lo stub del server pubblicato nel registry

        NotifyInterface callbackObj = new Notify(uStates);
        clientStub = (NotifyInterface) UnicastRemoteObject.exportObject(callbackObj, 0);

        main_menu.add("register");
        main_menu.add("unregister");
        main_menu.add("login");
        main_menu.add("exit");

        userLogged = "";

        while(true) {

            showMenu(main_menu);
            System.out.print("> ");
            String commandLine = scanner.nextLine();

            parseClientCommand(commandLine);

            System.out.print("\n");
        }

    }

    // invoca il metodo remoto per registrare un nuovo utente se non esiste già
    public static void register(String user, String password){
        try{
            System.out.println(serverStub.register(user,password));
        }catch(Exception e){
            System.out.println("> Error invoking object method " + e.toString() + e.getMessage());
        }
    }

    // invoca il metodo remoto per unregistrare un utente se esiste
    public static void unregister(String user, String password){
        try{
            System.out.println(serverStub.unregister(user,password));
        }catch(Exception e){
            System.out.println("> Error invoking object method " + e.toString() + e.getMessage());
        }
    }

    // metodo per ottenere stub del server
    public static void getROS(){
        // prendo il riferimento all'oggetto
        Remote remoteObject;
        try {
            Registry r = LocateRegistry.getRegistry(9999);
            remoteObject = r.lookup("WORTH-SERVER");
            serverStub = (RegisterInterface) remoteObject;
        }catch(RemoteException re){
            System.out.println("remote exception");
        }catch(NotBoundException ne){
            System.out.println("binding port exception");
        }
    }

    @SuppressWarnings("unchecked")
    public static void login(String username , String password) throws IOException, ClassNotFoundException {

        System.out.println("> Mi sto connettendo al server...");
        Socket server = new Socket(InetAddress.getLocalHost(), 6788);

        try(ObjectOutputStream os = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream  is = new ObjectInputStream(server.getInputStream());)
             {

            os.writeObject(username); // manda dati al server
            os.flush();
            os.writeObject(password);
            os.flush();

            String reply = "";

            //posso usare un bit
            reply = (String) is.readObject();
            if(reply.contentEquals("duplicate")){
                System.out.println("< error: l'utente è già connesso.");
                return;
            }
            else if (reply.contentEquals("no")) {
                System.out.println("< error: username o password non corretti. esco...");
                return;
            }

            System.out.println("< utente loggato con successo.");
            userLogged = username;

            // server manda lista di indirizzi...
            // creo una chat per ogni indirizzo ricevuto...
            // inizializzo le due strutture dati lato client
            projChats = new HashMap<String, ClientChat>();
            projInfo = new ProjectsInfo();


            HashMap<String, String> addresses;


            addresses = (HashMap<String, String>) is.readObject();

            for(String projName : addresses.keySet()) {
                String address = addresses.get(projName);
                projChats.put(address, new ClientChat(address, 9999));
                projInfo.addEntry(projName, address);
            }

            // 3)
            HashMap<String, Boolean> states = (HashMap<String, Boolean>) is.readObject();
            for(String uName : states.keySet()){
                uStates.getStates().put(uName, states.get(uName));
            }
            uStates.printStates();

            // 4) client appena loggato si registra per callback
            if(serverStub == null)
                getROS(); // ottiene server stub

            serverStub.registerForCallback(clientStub);
            //registra il client per callback.

            // 5) inizia la comunicazione
            startClientSession(is, os);

        }catch (IOException e)
        {
            System.err.println("errore nella connessione tcp");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void showMenu(ArrayList<String> menu){
        for(int i = 0; i < menu.size(); i++){
            String option = menu.get(i);
            System.out.print(i + 1 + ":" + option + " ");
        }
        System.out.print("\n");
    }

    public static void parseClientCommand(String commandLine) throws IOException, ClassNotFoundException {

        String[] tokens = commandLine.split(" ");

        String command = tokens[0];
        String[] args = new String[2];

        if(tokens.length > 1)
            args = Arrays.copyOfRange(tokens, 1, tokens.length);

        if(command.contentEquals("register") || command.contentEquals("login") || command.contentEquals("unregister")){

            if(args.length < 2 || args.length > 2){
                System.out.println("> usage: " + command + " <username> <password>");
                if(args.length == 0){
                    System.out.println("> error: manca lo username");
                }
                if(args.length == 1){
                    System.out.println("> error: manca la password");
                }
            }
            else {
                String username = args[0];
                String password = args[1];
                if (command.contentEquals("register"))
                    register(username, password);
                else if (command.contentEquals("unregister"))
                    unregister(username, password);
                else if (command.contentEquals("login"))
                    login(username, password);
            }
        }
        else if(command.contentEquals("exit"))
            exit();
        else System.out.println("> comando non valido.");

    }

    // inizio la sessione
    public static void startClientSession(ObjectInputStream is, ObjectOutputStream os) throws IOException, InterruptedException {

        boolean done = false;

        while(!done){

            System.out.print("("+ userLogged + ")> ");
            String commandLine = scanner.nextLine();

            String[] tok = commandLine.split(" ");

            String command = tok[0];

            String[] args = new String[0];

            if(tok.length > 1)
                args = Arrays.copyOfRange(tok, 1, tok.length);

            // alcuni comandi che vengono eseguiti anche o solo dal client
            if(command.contentEquals("readChat")){
                if(args.length != 1){
                    System.out.println("> ugage: readChat <project name>");
                    if(args.length == 0)
                        System.out.println("> error : specify the project name.");
                }
                else if(!projInfo.getDnsProjects().containsKey(args[0])){ // controllo se il client possiede il progetto
                    System.out.println("> error : il è progetto inesistente.");
                }
                else{
                String address = projInfo.getAddress(args[0]);
                for(String m : projChats.get(address).getClientMessages()){
                    System.out.println(m);
                }
                }
            }
            else if(command.contentEquals("sendMessage")){
                if(args.length < 2){
                    System.out.println("> ugage: sendMessage <project name> <messsage>");
                    if(args.length == 0)
                        System.out.println("> error : specify the project name.");
                    if(args.length == 1)
                        System.out.println("> error : enter a message.");
                }
                else if(!projInfo.getDnsProjects().containsKey(args[0])){
                    System.out.println("> error : il è progetto inesistente.");
                }
                else{
                    // ricostruisco il messaggio...
                    StringBuilder message = new StringBuilder("");
                    for(int i = 1; i<args.length; i++){
                        message.append(args[i] + " ");
                    }

                    String address = projInfo.getAddress(args[0]); // ottengo indirizzo della chat del progetto
                    projChats.get(address).writeMessage(userLogged, message.toString()); // lo mando
                }
            }else if(command.contentEquals("help")){ // mostra un helper
                System.out.println("///////////////////////////////////////////////////////////////////////////////////////\n");
                System.out.println("GESTIONE UTENTI:\n");
                System.out.println("listUsers : mostra lo stato degli utenti.");
                System.out.println("listOnlineUsers : mostra utenti online\n\n");
                System.out.println("GESTIONE PROGETTI E CARD:\n");
                System.out.println("createProject <projectName> : crea un nuovo progetto ");
                System.out.println("addMember <projectName> <username> : aggiunge un membro al progetto");
                System.out.println("showMembers <projectName> : mostra i membri di un progetto");
                System.out.println("addCard <projectName> <cardName> <description> : aggiunge una card a un progetto");
                System.out.println("showCards <projectName> : mostra le card di un progetto.");
                System.out.println("showCard <projectName> <cardName> : mostra info su una card");
                System.out.println("getCardHistory <projectName> <cardName> : mostra history di una card\n");
                System.out.println("moveCard <projectName> <cardName> <sourceList> <destList> :");
                System.out.println("sposta una card dalla sourceList alla destList.");
                System.out.println("Utilizza SOLO i seguenti nomi per le liste:");
                System.out.println("todoList, inprogList, reviseList, doneList\n");
                System.out.println("removeProject <projectName> : rimuove un progetto.\n\n");
                System.out.println("CHAT:\n");
                System.out.println("sendMessage <projectName> <message> : manda un messaggio sulla chat.");
                System.out.println("readMessage <projectName> : leggi i messaggi sulla chat non letti.\n\n");
                System.out.println("logout : esegui il logout\n");
                System.out.println("///////////////////////////////////////////////////////////////////////////////////////");
            }
            else {
                os.writeObject(command);
                os.flush();

                os.writeObject(args);
                os.flush();

                try {
                    String reply = (String) is.readObject();
                    if (reply.contentEquals("stop")) {
                        done = true;

                        if (projChats != null)
                            closeChats();

                        serverStub.unregisterForCallback(clientStub);

                    } // keyword per gestire logout
                    else if(reply.contentEquals("address")) { // il server mi manderà un indirizzo e nome per chat

                        String name = (String) is.readObject();
                        String address = (String) is.readObject();

                        System.out.println("< new project has been created");

                        // aggiunta la chat del nuovo progetto...
                        projChats.put(address, new ClientChat(address, 9999));
                        projInfo.addEntry(name, address);

                    } // keyword per acquisire indirizzo di una chat
                    else if(reply.contentEquals("unaddress")) {
                        String projectName = args[0];

                        String address = projInfo.getAddress(projectName);
                        projInfo.removeEntry(projectName);

                        projChats.get(address).closeChat(); // interrompo il reader
                        projChats.remove(address);

                        System.out.println("< progetto è stato eliminato correttamente");
                    } // keyword per eliminare una chat
                    else
                        System.out.println("< " + reply);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // chiamata quando l'utente fa logout...
    public static void closeChats(){

        // chiudo tutti i reading threads
        for(String address : projChats.keySet()){
            projChats.get(address).closeChat();
        }

        projChats = null;
    }

    public static void exit(){

        if(projChats != null){
            closeChats();
        }

        System.out.println("exiting from worth!");

        try{Thread.sleep(500);}
        catch (InterruptedException e) {}

        System.exit(0);
    }


}

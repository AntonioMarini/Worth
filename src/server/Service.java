package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public class Service implements Runnable{

    private int userIndex; // index in uData dell' utente loggato
    private String userNameLogged; //  username dell' utente loggato

    private UsersData uData; // database degli user di worth
    private ProjectData pData; // database dei progetti di worth

    private Socket clientSock; // socket per la comunicazione col client

    private static boolean clientDone;

    private RegisterImpl reg;

    public Service(UsersData uData, ProjectData pData, Socket clientSock, RegisterImpl reg){
        this.uData = uData;
        this.pData = pData;
        this.clientSock = clientSock;
        this.reg = reg;
    }

    @Override
    public void run() {
        loginListen();
    }

    // 1) identificazione attraverso username e password. se fallisce chiude la connessione.
    // 2) manda indirizzi delle chat dei progetti dell'utente.
    // 3) manda dati al client reg.getUsersStates()
    // 4) il client viene registrato per callback sugli stati utente (es. "antonio è ora online")
    // 5) su questa connessione tcp verranno gestite tutte le altre call del client.
    // 6) quando l' utente chiamerà logout imposto done a true -> la connessione viene chiusa.
    public void loginListen()  {

            try(ObjectOutputStream os = new ObjectOutputStream(clientSock.getOutputStream());
                ObjectInputStream  is = new ObjectInputStream(clientSock.getInputStream());
                ){

            String username = (String) is.readObject();
            String password = (String) is.readObject();

            if(uData.check(username, password)){ // 1)
        
                System.out.println("(" + username + "):" + " identificato con successo.");

                userNameLogged = username;

                os.writeObject("yes");
                os.flush();

                User userLogged = uData.get(username);
                this.userIndex = uData.getUsers().indexOf(userLogged);
                uData.setUserState(uData.get(userIndex), true);

                // 2) send chat addresses...
                os.writeObject(pData.getProjectsAddr(userLogged));

                //3
                HashMap<String, Boolean> userStates = uData.getUsersStates();
                os.writeObject(userStates);


                //4) invio callback
                reg.updateState(userNameLogged, true);

                // 5) apro ciclo di attesa comandi
                clientDone = false;
                while(!clientDone){
                    // rimani in attesa di comandi
                    // si blocca finchè non legge comando dal client
                    System.out.println("(" + username + "):" + " in attesa di comandi.");

                    String command = (String) is.readObject();
                    String[] args = (String[]) is.readObject();

                    dispatchCommand(command, args, os);
                }
                uData.setUserState(uData.get(userIndex), false); // stato impostato offline
                System.out.println("(" + username + "):" + " logged out; connessione chiusa.");
            }else if(uData.get(username) == null){ // se l'utente non esiste
                os.writeObject("no"); 
                os.flush();
            }else if(uData.get(username).getState()){  // se l'utente è già online
                os.writeObject("duplicate"); // invio esito duplicato (errore)
                os.flush();
            }else {
                os.writeObject("no"); // invio esito username o password not matching
                os.flush();
            }
            }catch (IOException | ClassNotFoundException e){e.printStackTrace();}
        }

    // metodo che legge il comando inviato dal client,
    // ne controlla gli argomenti,
    // invoca il relativo metodo,
    // e invia al client l'esito
    public void dispatchCommand(String command, String[] args, ObjectOutputStream os) {

        String projectMissingError = "error : specifica il nome del progetto!\n";
        String cardMissingError = "error : specifica il nome della card!\n";
        String usernameMissingError = "error: specifica lo username!";

        StringBuilder reply = new StringBuilder("");

        if(command.contentEquals("logout")){
            if(args.length != 0){
                reply.append("ugage: logout");
            }else {
                clientDone = true;
                try {
                    reg.updateState(userNameLogged, false);
                }catch(RemoteException e){
                    e.printStackTrace();
                }
                reply.append("stop");
            }
        }
        else if(command.contentEquals("listUsers")){
            if(args.length != 0){
                reply.append("ugage: listUsers");
            }else
                reply.append(listUsers());
        }
        else if(command.contentEquals("listOnlineUsers")){
            if(args.length != 0){
                reply.append("ugage: listOnlineUsers");
            }else
                reply.append(listOnlineUsers());
        }
        else if(command.contentEquals("listProjects")){
            if(args.length != 0){
                reply.append("ugage: listProjects");
            }else
            reply.append(listProjects());
        }
        else if(command.contentEquals("createProject")){
            if(args.length < 1 || args.length > 1){
                reply.append("ugage: createProject <project name>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
            }
            else{
                if(pData.contains(args[0])) //se il progetto esiste già
                    reply.append("error: il progetto esiste già!");
                else {
                    createProject(args[0]);
                    //mando al client indirizzo chat del nuovo progetto
                    try {
                        os.writeObject("address");
                        os.writeObject(pData.getProject(args[0]).getName());
                        os.writeObject(pData.getProject(args[0]).getAddress());
                    } catch (IOException io) {
                        io.printStackTrace();
                    }

                    reply.append("noreply");
                }
            }
        }
        else if(command.contentEquals("removeProject")) {
            if(args.length < 1 || args.length > 1){
                reply.append("ugage: removeProject <nome_progetto>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
            }else{
                String projectName = args[0];
                if(!pData.contains(projectName))
                    reply.append(projectName + "non esiste.\n");
                else {
                    reply.append(removeProject(projectName));
                }
            }
        }
        else if(command.contentEquals("addMember")){
            if(args.length < 2 || args.length > 2){
                reply.append("ugage: addMember <project name> <username>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
                if(args.length == 1)
                    reply.append("error : specifica l'utente da aggiungere!\n");
            }
            else
                reply.append(addMember(args[0], args[1]));
        }
        else if(command.contentEquals("showMembers")){
            if(args.length < 1 || args.length > 1){
                reply.append("ugage: showMembers <project name>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
            }else
                reply.append(showMembers(args[0]));
        }
        else if(command.contentEquals("showCards")){
            if(args.length < 1 || args.length > 1){
                reply.append("ugage: showCards <project name>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
            }else
                reply.append(showCards(args[0]));
        }
        else if(command.contentEquals("showCard")){
            if(args.length < 2  || args.length > 2){
                reply.append("ugage: showCard <project name> <card name>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
                if(args.length == 1)
                    reply.append(cardMissingError);
            }else
                reply.append(showCard(args[0], args[1]));
        }
        else if(command.contentEquals("addCard")){
            if(args.length < 3){
                reply.append("ugage: addCard <project name> <card name> <description>\n");
                if(args.length == 0)
                    reply.append(projectMissingError);
                if(args.length == 1)
                    reply.append(cardMissingError);
                if(args.length == 2)
                    reply.append("error : specifica una piccola descrizione della card.\n");
            }
            else {
                //ricostruisco la descrizione
                StringBuilder description = new StringBuilder("");
                for(int i = 2; i < args.length; i++){
                    description.append(args[i] + " ");
                }
                reply.append(addCard(args[0], args[1], description.toString()));
            }
        }
        else if(command.contentEquals("moveCard")){
            if(args.length < 4 || args.length > 4){
                reply.append("ugage: moveCard <project name> <card name> <source list> <dest list>\n");
            }
            else
                reply.append(moveCard(args[0], args[1], args[2], args[3]));
        }
        else if(command.contentEquals("getCardHistory")){
            if(args.length < 2 || args.length > 2){
                reply.append("error: usage: getCardHistory <project name> <card name>");
                if(args.length == 0)
                    reply.append(projectMissingError);
                if(args.length == 1)
                    reply.append(cardMissingError);

            }
            else
                reply.append(getCardHistory(args[0], args[1]));
        }
        else reply.append("comando non valido");

        if(!reply.toString().contentEquals("noreply")){ // nel caso in cui ho già inviato la risposta
        try {
            os.writeObject(reply.toString()); // invio l'esito al client
        }catch(IOException e){e.printStackTrace();}
        }
    }

    // vari semplici metodi wrappers chiamati dal dispatcher per il PRINTING degli esiti
    public boolean checkPermissions(String projectName){
        for(User u : pData.getProject(projectName).showMembers()){
            if(u.getUsername().contentEquals(userNameLogged)){
                return true;
            }
        }
        return false;
    }

    public String listUsers(){

        StringBuilder res = new StringBuilder();

        HashMap<String, Boolean> states = uData.getUsersStates();

        for(String uname: states.keySet()){
            String state = "offline";
            if(states.get(uname).booleanValue() == true) state = "online";
            res.append(uname + " is " + state + "  \n");
        }

        return res.toString();
    }

    public String listOnlineUsers(){

        StringBuilder res = new StringBuilder();

        HashMap<String, Boolean> states = uData.getUsersStates();

        for(String uname: states.keySet()){
            if(states.get(uname).booleanValue() == true)
                res.append(uname + "  \n");
        }

        return res.toString();
    }

    public String listProjects(){
        StringBuilder res = new StringBuilder();

        ArrayList<String> userProjects = uData.get(userIndex).getProjects();;

        for(String projName : userProjects){
            res.append(projName + "\n  ");
        }

        return res.toString();
    }

    public void createProject(String projectName){
        String chatAddress = genChatAddress();
        uData.get(userIndex).addToProject(projectName);
        uData.saveUser(uData.get(userIndex));
        pData.addProject(projectName, uData.get(userIndex), chatAddress);
    }

    public String genChatAddress(){
        int i = 0;

        // se ci sono buchi tra gli indirizzi li assegna
        for (Project p : pData.getProjects()){
            if(!p.getAddress().contentEquals("228.1.2." + i))
                return "228.1.2." + i;
            i++;
        }

        if(i>255)
            return "full";

        return "228.1.2." + i+1; // non ci sono buchi assegna l'ultimo disponibile.
    }

    public String removeProject(String projectName){

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        if(!pData.getProject(projectName).isDone()){ // se le card sono tutte nella donelist
            return "impossibile eliminare il progetto: ci sono ancora card da completare!";
        }

        for(User u : pData.getProject(projectName).showMembers()){
            uData.get(u.getUsername()).removeFromProject(projectName);
            uData.saveUser(u);
        }
        pData.removeProject(projectName);

        return "unaddress";
    }

    public String addMember(String projectName, String username){

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        User u;
        if( (u = uData.get(username)) == null)
            return username + " non esiste.";

        if(pData.getProject(projectName).showMembers().contains(uData.get(username)))
            return username + " è gia membro del progetto" + projectName;

        pData.getProject(projectName).addMember(u);
        uData.get(username).addToProject(projectName);
        uData.saveUser(uData.get(username));

        writeServerMessage(projectName, username + " è stato aggiunto al progetto");
        return username + " è stato aggiunto al progetto " + projectName;

    }

    public String showMembers(String projectName){

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        StringBuilder res = new StringBuilder("");

        for(User u : pData.getProject(projectName).showMembers()){
            res.append(u.getUsername() + "\n  ");
        }

        return res.toString();

    }

    public String showCards(String projectName){

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        StringBuilder res = new StringBuilder("");

        for(Card c : pData.getProject(projectName).showCards())
            res.append(c.getName() + "\n  ");

        return res.toString();
    }

    public String showCard(String projectName, String cardName){

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        StringBuilder res = new StringBuilder("");

        Card c = pData.getProject(projectName).showCard(cardName);
        if(c == null)
            return "la card " + cardName + " non esiste.";

        res.append(c.getName() + "(" + c.getState() + ") :\n");
        res.append("  " + c.getDescription() + "\n");
        return res.toString();
    }

    public String addCard(String projectName, String cardName, String description){

        String res;

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";


        Card c = pData.getProject(projectName).showCard(cardName);
        if(c != null)
            return "la card " + cardName + " esiste già.";

        //se non esiste viene creata
        pData.getProject(projectName).addCard(cardName, description);
        res = "la card " + cardName + " è stata aggiunta al progetto " + projectName;
        writeServerMessage(projectName, res); // server lo scrive nella chat

        return res;
    }

    public String moveCard(String projectName, String cardName, String sourceList, String destList){

        String res = "";

        if(!pData.contains(projectName))
            return " il progetto non esiste " + projectName + " non esiste";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        int rcode = pData.getProject(projectName).moveCard(cardName, sourceList, destList);

        if(rcode == 2)
            res = "error: non puoi spostare la card da " + sourceList + " a " + destList;
        if(rcode == -1)
            res = "error: la card " + cardName + " non esiste";
        if(rcode == 1) {
            res = "la card " + cardName + " è stata spostata dalla " + sourceList + " alla " + destList;
            writeServerMessage(projectName, res); // server scrive sulla chat
            // salvo progetto dopo una sua modifica
        }
        return res;
    }

    public String getCardHistory(String projectName, String cardName){

        String res;

        if(!pData.contains(projectName))
            return projectName + " non esiste.";

        if(!checkPermissions(projectName))
            return "non hai i permessi per eseguire questa operazione.";

        Card c = pData.getProject(projectName).showCard(cardName);
        if(c == null)
            return "la card " + cardName + " non esiste.";

        res = c.getHistory();
        return res;
    }

    // metodo che server utilizza per scrivere sulla chat
    public void writeServerMessage(String projectName, String text){
        pData.getProject(projectName).writeMessage(text);
    }

}

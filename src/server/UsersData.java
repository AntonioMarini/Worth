package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

// struttura dati che contiene tutti gli utenti.
public class UsersData {

    private Object usersMutex = new Object();
    private ArrayList<User> users;

    private Object fileMutex = new Object(); // accesso ai file sincronizzato per la scrittura (solo quando salvo)

    private static final String dirpath = "res/Users";;

    public UsersData(){
        this.users = parseUsers(); // chiamata la parseUsers per riempire users dal file.
    }

    // UsersData
    public ArrayList<User> getUsers(){
        return this.users;
    }

    public boolean contains(String username){
        return users.contains(username);
    }
    public boolean contains(User user) {return users.contains(user);}

    // controlla se esiste un utente con username e password corrispondenti
    public boolean check(String username, String password){
        if(get(username) == null) return false;

        User tmp = get(username);

        return(tmp.getPassword().contentEquals(password));
    }

    // aggiunge utente alla liste degli utenti
    public void addUser(User user){
        synchronized (usersMutex) {
            users.add(user); // sincronizzato
        }
        saveUser(user);
    }

    //ritorna utente con indice index in this.users
    public User get(int index){
        return users.get(index);
    }

    //ritorna utente con username corrrispondente
    public User get(String username){
        for(User u: users){
            if(u.getUsername().contentEquals(username)){
                return u;
            }
        }
        return null;
    }

    //rimuove un utente da this.users -> synchronized block
    public void removeUser(User user){
        synchronized (usersMutex) {
            users.remove(user);
        }
        deleteUserFile(user.getUsername());
    }

    // ritorna hashmap con key = username e value = stato dell'utente
    public HashMap<String, Boolean> getUsersStates(){
        HashMap<String, Boolean> userStates = new HashMap<>();

        for( User u : users){
            userStates.put(u.getUsername(), u.getState());
        }

        return userStates;
    }

    // ritorna lista dei membri del progetto quando viene ricostruito
    public ArrayList<User> getProjectMembers(String projectName){
        ArrayList<User> res = new ArrayList<>();
        for(User u : users){
            for(String projName : u.getProjects()){
                if(projName.contentEquals(projectName)){
                    res.add(u);
                }
            }
        }

        return res;
    }

    // imposta lo stato di utente (wrapper per User.setState())
    public void setUserState(User user, boolean state){
        User u = users.get(users.indexOf(user));
        u.setState(state); // già sincronizzato nella classe User
    }

    // parsa gli utenti dai loro file al programma
    // viene chiamata solo una volta (nella creazione) -> non ha senso sincronizzarla
    public ArrayList<User> parseUsers() {

        ArrayList<User> users = new ArrayList<>();
        File dir = new File(dirpath);
        //se file non esiste ne creo uno nuovo...

        if (!dir.exists())
        {
            dir.mkdirs();
            System.out.println("Cartella Users non esiste. Ne creo una nuova.");
            return users;
        }

        for(File usrFile : dir.listFiles()){
            try (FileInputStream fis = new FileInputStream(usrFile);
                 ObjectInputStream is = new ObjectInputStream(fis)) {

                    User u = (User) is.readObject();
                    u.initializemutexes(); 
                    u.setState(false);
                    users.add(u);

            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                System.err.println("error while parsing users");
            }
        }

        return users;
    }

    //salva utente nel file; chiamata quando qualcuno si registra o si unregistra.
    public void saveUser(User u){

        synchronized (fileMutex) {
        File file = new File(dirpath + "/" + u.getUsername() + ".usr");
            try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))) {
                os.writeObject(u);
            } catch (IOException ioex) {
                System.err.println("error while saving user to file");
            }
        }
    }

    // elimina il file di un utente
    public void deleteUserFile(String username){

        File file = new File(dirpath + "/" + username + ".usr");
        if(file.exists()) {
            synchronized (fileMutex) {
                file.delete();
            }
        }
    }

}

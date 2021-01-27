package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ProjectData {

    private Object fileMutex = new Object();
    private static final String dirname = "res/Projects";

    private Object projectsListMutex = new Object();
    private ArrayList<Project> projectsList;

    public ProjectData(UsersData uData){
        projectsList = parseProjects(uData); // ho bisogno di informazioni degli utenti per ricostruire i progetti
        initializeChats();
    }

    // ritorna la lista di tutti i progetti... // todo forse meglio un clone
    public ArrayList<Project> getProjects(){return this.projectsList;}

    // ritorna una lista dei progetti di un utente
    private ArrayList<Project> listProjects(User u){

        ArrayList<Project> userProjects = new ArrayList<>();

        for(Project proj : projectsList){
            if(proj.showMembers().contains(u)){
                userProjects.add(proj);
            }
        }

        return userProjects;
    }

    // aggiunge progetto alla lista (crea anche la sua chat) -> synchronized block
    public void addProject(String projectName, User user, String chatAddress){
        Project project = new Project(projectName, user, chatAddress );
        synchronized (projectsListMutex) {
            if(!projectsList.contains(project))
                this.projectsList.add(project);
        }
        //lo salvo nel suo file
        saveProject(projectName);
    }

    // rimuove il progetto da projects e dalla cartella Projects
    public void removeProject(String projectName){
        synchronized (projectsList) {
            if(contains(projectName)) {
                getProject(projectName).closeChat(); // 1) interrompo il thread reader della chat
                removeProjectFiles(projectName);// 2) rimuovo il file di progetto.
                projectsList.remove(getProject(projectName)); // 3) lo rimuovo dalla lista; progetto finisce nel garbage :)
            }
        }
    }

    // ritorna true se esiste il progetto con nome projectname
    public boolean contains(String projectName){
        for(Project p : projectsList){
            if(p.getName().contentEquals(projectName))
                return true;
        }
        return false;
    }

    //ritorna progetto con nome projectname se esiste, altrimenti null
    public Project getProject(String projectName){
        for(Project p : projectsList){
            if(p.getName().contentEquals(projectName))
                return p;
        }
        return null;
    }

    // ritorna lista di indirizzi delle chat di un utente
    public HashMap<String, String> getProjectsAddr(User user){

        HashMap<String, String> res = new HashMap<>();

        ArrayList<Project> tmp = listProjects(user);

        for(Project p : tmp){
            res.put(p.getName(), p.getAddress());
        }
        return res;
    }

    // inizializza le chat di tutti i progetti (chaiamata solo all'inizio del server)
    public void initializeChats(){
        for(Project p : projectsList){
            p.startChat();
        }
    }

    // chiude le chat di tutti i progetti
    public void closeChats(){
        for(Project p : projectsList){
            p.closeChat();
        }
    }

    // recupera i progetti dai file nella cartella Projects se ci sono
    public ArrayList<Project> parseProjects(UsersData uData){

        System.out.println("Importo i progetti...");

        ArrayList<Project> projects = new ArrayList<>();

        File projectsDir = new File(dirname);

        if(!projectsDir.exists()) {
            projectsDir.mkdirs();
            return projects;
        }

        int index = 1;
        for(File  projDir: projectsDir.listFiles()){

            String address = "228.1.2." + index++; // genero indirizzo della chat univoco.
            ArrayList<Card> tmp = new ArrayList<Card>(); // lista temp dove mettere le card lette
            ArrayList<User> members = uData.getProjectMembers(projDir.getName()); // interrogo uData per recuperare i membri

            for(File cardFile : projDir.listFiles()){ // 3) recupero lista cards
                try(ObjectInputStream is = new ObjectInputStream(new FileInputStream(cardFile))){
                      Card c = (Card) is.readObject(); // leggo da file la card (deserializzo)
                      c.initializemutexes(); //inizializzo i mutex della card per la sincronizzazione
                      tmp.add(c); // la aggiungo alla lista delle cards
                }catch (IOException e){
                    e.printStackTrace();
                    System.err.println("error parsing projects");
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                    System.err.println("class not found.");
                }
            }

            Project p = new Project(projDir.getName(), members, tmp, address);// 4) ricreo il progetto

            projects.add(p); // lo aggiungo alla lista dei progetti
        }

        return projects;
    }

    // chiamata dopo la creazione di un progetto -> crea la cartella di progetto.
    public void saveProject(String projectName){
        synchronized (fileMutex) {
            Project proj = getProject(projectName);
            if (proj != null) {
                //se cartella di progetto non esiste ne creo una
                File projectDir = new File(dirname + "/" + projectName);
                if (!projectDir.exists()) projectDir.mkdirs();

            }
        }
    }

    // rimuovo tutti i file associati a un progetto che viene eliminato
    public void removeProjectFiles(String projectName){

        File proj = new File( dirname + "/" + projectName);
        File[] cards = proj.listFiles();

        for(File c : cards){
            if(!c.exists()){
                System.out.println(c.getName() + "non esiste!");
            }else if (c.delete()) {
                //
            } else {
                System.out.println("Failed to delete the file.");
            }
        }
        proj.delete();
    }
}

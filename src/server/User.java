package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class User implements Serializable {


    static final long serialVersionUID = 6378070323726112292L;
    private String username;
    private String password;

    private transient Object stateMutex = new Object();
    private boolean isOnline;

    private transient Object projectsMutex = new Object();
    private ArrayList<String> projects; // lista dei progetti dell'utente

    public User(String username, String password){
        this.username = username;
        this.password = password;
        this.projects = new ArrayList<>();
        this.isOnline = false;
    }

    public void initializemutexes(){
        stateMutex = new Object();
        projectsMutex = new Object();
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public Boolean getState() { return this.isOnline;}

    public void setState(boolean state) {
        synchronized(stateMutex){
            this.isOnline = state;
        }
    }

    public void addToProject(String projectName){
        synchronized (projectsMutex) {
            if(projectName != null && projectName != null)
                projects.add(projectName);
        }
    }

    public void removeFromProject(String projectName) {
        synchronized (projectsMutex) {
            this.projects.remove(projectName);
        }
    }

    public ArrayList<String> getProjects() {
        return projects;
    }

    // content equality override
    // ( due utenti considerati uguali se hanno stesso username:
    @Override
    public boolean equals(Object o) {
        User user = (User) o;
        return username.contentEquals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }


}

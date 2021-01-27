package client;

import java.util.HashMap;

public class ProjectsInfo {

    private HashMap<String, String> dnsProjects;

    public ProjectsInfo(){
        this.dnsProjects = new HashMap<>();
    }

    public void addEntry(String projName, String projAddr){
        dnsProjects.put(projName, projAddr);
    }

    public void removeEntry(String projName){
        dnsProjects.remove(projName);
    }

    public String getAddress(String projName){
        return dnsProjects.get(projName);
    }

    public HashMap<String, String> getDnsProjects() {
        return dnsProjects;
    }
}

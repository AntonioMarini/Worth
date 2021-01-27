package client;

import java.util.HashMap;


public class UsersStates {

    private HashMap<String , Boolean> states;

    public UsersStates(){
        this.states = new HashMap<>();
    }

    public HashMap<String, Boolean> getStates(){
        return this.states;
    }

    public void setState(String username, boolean state){
        states.put(username, state);
    }

    public void printStates(){
        for(String uname: states.keySet()){

            String state = "offline";
            if(states.get(uname).booleanValue() == true) state = "online";

            System.out.println(uname + " is " + state);
        }
    }

    public void printState(String username){
        String state = "offline";
        if(states.get(username).booleanValue() == true) state = "online";

        System.out.println(username + " is now " + state);
        System.out.print("> ");
    }

}

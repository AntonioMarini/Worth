package server;

import java.io.*;

/* 
//
// Classe che rappresenta una card di un progetto.
// @author Antonio Marini
*/
public class Card implements Serializable {

    static final long serialVersionUID = 8239976473768659909L;
    private String name; // nome della card
    private String description; // descrizione della card
    private String projectName;

    private transient Object stateMutex = new Object();
    private String state; // indica lo stato della card riferito alle 4 liste del progetto

    private String history; // history degli stati della card

    // costruttore della card, dopo averla inizializzata crea il file e la salva
    public Card(String name, String description, String projectName){
        this.name = name;
        this.state = "todo";
        this.description = description;
        this.history = "todo";
        this.projectName = projectName;

        saveCard();
    }

    // metodo per inizializzare i mutex per la sincronizzazione
    public void initializemutexes(){
        stateMutex = new Object();
    }

    // ritorna la descrizione della card
    public String getDescription() {
        return description;
    }

    // ritorna il nome della card
    public String getName() {
        return name;
    }

    // ritorna lo stato della card
    public String getState() { return state; }

    // ritorna la storia della card
    public String getHistory() {
        return history;
    }

    // aggiorna la storia della card (usato solo da setState())
    private void updateHistory(String update) {this.history += " - " + update;}

    // imposta lo stato della card {todo, inprog, revise, done}
    public void setState(String state) {
        synchronized (stateMutex) {
            this.state = state;
            updateHistory(state);
            saveCard();
        }
    }

    // chiamata quando lo stato di una card cambia
    public void saveCard(){

            File file = new File("res/Projects/" + projectName + "/" + this.name + ".card");

            try(FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fos)){
                //scrivo la card sul file (serializzata)
                os.writeObject(this);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
    }
}

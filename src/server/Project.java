package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


/* 
//
// Classe che rappresenta un progetto.
// I suoi file si trovano in ./res/Projects/projectname/
// @author Antonio Marini
*/
public class Project implements Serializable {


    static final long serialVersionUID = 5209536258001887158L;
    private final String name;

    private Object membersMutex ;
    private ArrayList<User> members; // lista di membri del progetto

    private Object cardsMutex;
    private List<Card> allCards;

    private Object todoMutex;
    private List<Card> todoList;

    private Object inprogMutex;
    private List<Card> inprogList;

    private Object reviseMutex;
    private List<Card> reviseList;

    private Object doneMutex;
    private List<Card> doneList; 

    private Chat chat; // chat del progetto
    private String chatAddress;  // indirizzo della chat

    // crea un nuovo progetto con nome name, aggiunge ai membri il creatore
    public Project(String name, User creator, String chatAddress){
        this.name = name;

        this.members = new ArrayList<>();
        this.members.add(creator); // creator verrà aggiunto di default ai membri del progetto. (come da specifica)

        this.todoList = new ArrayList<>();
        this.inprogList = new ArrayList<>();
        this.reviseList = new ArrayList<>();
        this.doneList = new ArrayList<>();

        this.allCards = new ArrayList<>();

        this.chatAddress = chatAddress;

        initializemutexes();
        startChat();
    }

    // costruttore per progetti importati da file
    public Project(String name, ArrayList<User> members,  List<Card> allCards, String chatAddress){
        this.name = name;

        this.members = members;

        this.chatAddress = chatAddress;

        this.allCards = allCards;

        this.todoList = new ArrayList<>();
        this.inprogList = new ArrayList<>();
        this.reviseList = new ArrayList<>();
        this.doneList = new ArrayList<>();

        initializemutexes();

        // aggiungo la card alla sua lista di stato
        Iterator<Card> itCards = allCards.iterator();
        while(itCards.hasNext()){
            Card c = itCards.next();
            String listName = c.getState() + "List";

            // la aggiungo alla lista a cui è associato lo stato
            addCardToList(c, listName);
        }
        
        startChat();
    }

    // inizializza i mutex per la sincronizzazione
    private void initializemutexes(){
        membersMutex = new Object();
        cardsMutex = new Object();
        todoMutex = new Object();
        inprogMutex = new Object();
        reviseMutex = new Object();
        doneMutex = new Object();
    }

    // ritorna indirizzo della chat del progetto
    public String getAddress() {
        return chatAddress;
    }

    // ritorna la lista di card listname
    public List<Card> getCardList(String listName){
        if(listName.contentEquals("todoList"))
            return todoList;
        if(listName.contentEquals("inprogList"))
            return inprogList;
        if(listName.contentEquals("reviseList"))
            return reviseList;
        if(listName.contentEquals("doneList"))
            return doneList; 
        
        return null;
    }

    // inizializza la chat del progetto -> start del thread reader
    public void startChat(){
        this.chat = new Chat(chatAddress, 9999);
    }

    // chiude la chat del progetto -> stop del thread reader
    public void closeChat(){
        if(chat != null) {
            chat.closeChat();
            chat = null;
        }
    }

    // ritorna nome del progetto
    public String getName() { return name; }

    // aggiunge un nuovo membro al progetto
    public void addMember(User user){
        synchronized (membersMutex) {
            this.members.add(user);
        }
    }

    // ritorna lista di utenti membri del progetto
    public ArrayList<User> showMembers() {
        return members;
    }

    // ritorna lista di card del progetto
    public ArrayList<Card> showCards(){
        return (ArrayList<Card>) allCards;
    }

    // ritorna card con nome cardname se c'è altrimenti ritorna null
    public Card showCard(String cardName){
        
        for(Card c : allCards){
            if(c.getName().contentEquals(cardName))
                 return c;
        }
        
        return null;
    }

    // aggiunge una nuova card al progetto
    public void addCard(String name, String description){
        Card card = new Card(name, description, this.name);
        
        synchronized(todoMutex){
            todoList.add(card); //aggiunta automaticamente alla lista Todo
        }

        synchronized (cardsMutex){
            allCards.add(card); // aggiunta alla lista di tutte le card
        }
    }

    // aggiunge una card alla lista listname
    private void addCardToList(Card c, String listName){
        // aggiungo la card alla lista listname
        synchronized(getListMutex(listName)){
            getCardList(listName).add(c);
        }
    }

     // ritorna il mutex associato a una lista listname
    private Object getListMutex(String listName){
        if(listName.contentEquals("todoList")){
            return todoMutex;
        }else if(listName.contentEquals("inprogList")){
            return inprogMutex;
        }
        else if(listName.contentEquals("reviseList")){
            return reviseMutex;
        }
        else if(listName.contentEquals("doneList")){
            return doneMutex;
        }
        else return null;
    }

    // metodo privato: rimuove e ritorna la card dal progetto nella lista listname
    private Card removeCard(String cardName, String listName){
    Card res;
    List<Card> list = getCardList(listName);
    for(Card c : list){
        if(c.getName().contentEquals(cardName)){
            synchronized (getListMutex(listName)) {
                if(c != null){
                    res = c;
                    list.remove(c); // lo rimuovo
                    return res;
                }
            }
        }
    }
    return null;
}
    
    // sposta card da una lista a un'altra (seguendo le regole kanban)
    public int moveCard(String cardName, String sourceList, String destList){

        //operazioni valide
        if(     (sourceList.contentEquals("todoList")   && destList.contentEquals("inprogList")) ||
                (sourceList.contentEquals("inprogList") && destList.contentEquals("reviseList")) ||
                (sourceList.contentEquals("inprogList") && destList.contentEquals("doneList"))   ||
                (sourceList.contentEquals("reviseList") && destList.contentEquals("doneList"))   ||
                (sourceList.contentEquals("reviseList") && destList.contentEquals("inprogList"))     )
        {
            Card c = removeCard(cardName, sourceList);
            if(c == null) return -1; // codice errore: card non presente.

            //aggiorno lo stato della card e la sua history
            String newCardState = destList.substring(0, destList.length() - 4);
            c.setState(newCardState);  // setto lo stato della card (blocco già sincronizzato)

            synchronized (getListMutex(destList)) {
               getCardList(destList).add(c);
            }

            return 1; // succesful.

        } else return 2; // codice errore: operazioni invalide con le liste.

    }

    // ritorna true se tutte le card sono nella doneList
    public boolean isDone(){
        return(allCards.size() == doneList.size());
    }

    // scrive un messaggio sulla chat
    public void writeMessage( String text){
        if(chat != null)
            chat.writeMessage(text);
        else System.err.println("(debug) la chat non è stata inizializzata");
    }
}

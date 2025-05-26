package com.azienda.timbratura.model;

 import java.sql.Date;
 import java.sql.Time;

 public class Timbratura {

     private int id;
     private String codiceRfidUtente;
     private String codiceTotem;
     private Date giorno;
     private Time ora;

     public Timbratura(int id, String codiceRfidUtente, String codiceTotem, Date giorno, Time ora) {
         this.id = id;
         this.codiceRfidUtente = codiceRfidUtente;
         this.codiceTotem = codiceTotem;
         this.giorno = giorno;
         this.ora = ora;
     }

     // Getters
     public int getId() { return id; }
     public String getCodiceRfidUtente() { return codiceRfidUtente; }
     public String getCodiceTotem() { return codiceTotem; }
     public Date getGiorno() { return giorno; }
     public Time getOra() { return ora; }

     /**
      * Ritorna una rappresentazione JSON semplificata dell'oggetto.
      */
     public String toJson() {
         return String.format("{\"rfid\": \"%s\", \"totem\": \"%s\", \"giorno\": \"%s\", \"ora\": \"%s\"}",
                 codiceRfidUtente, codiceTotem, giorno.toString(), ora.toString());
     }
 }
package com.azienda.timbratura.db;

 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.SQLException;

 public class DatabaseManager {

     // !!! MODIFICA QUESTI VALORI CON I TUOI !!!
     private static final String DB_URL = "jdbc:mysql://localhost:3306/timbratura_azienda?serverTimezone=UTC";
     private static final String DB_USER = "root"; // Il tuo utente DB
     private static final String DB_PASSWORD = ""; // La tua password DB
     // !!! --------------------------------- !!!

     static {
         try {
             Class.forName("com.mysql.cj.jdbc.Driver");
         } catch (ClassNotFoundException e) {
             throw new RuntimeException("Errore: Driver MySQL non trovato!", e);
         }
     }

     public static Connection getConnection() throws SQLException {
         return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
     }
 }
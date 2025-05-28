package com.azienda.timbratura.db;

 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.SQLException;

 public class DatabaseManager {


     private static final String DB_URL = "jdbc:mysql://localhost:3306/timbratura_azienda?serverTimezone=UTC";
     private static final String DB_USER = "root"; 
     private static final String DB_PASSWORD = ""; 

     // crea una classe per il drive della java db connection
     static {
         try {
             Class.forName("com.mysql.cj.jdbc.Driver");
         } catch (ClassNotFoundException e) {
             throw new RuntimeException("Errore: Driver MySQL non trovato!", e);
         }
     }

     // crea un oggetto connection usando la classe driveManager e i dati
     public static Connection getConnection() throws SQLException {
         return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
     }
 }
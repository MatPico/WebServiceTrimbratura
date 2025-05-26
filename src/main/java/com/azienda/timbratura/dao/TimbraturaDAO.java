package com.azienda.timbratura.dao;

 import com.azienda.timbratura.db.DatabaseManager;
 import com.azienda.timbratura.model.Timbratura;

 import java.sql.*;
 import java.time.LocalDate;
 import java.time.LocalTime;
 import java.util.ArrayList;
 import java.util.List;

 public class TimbraturaDAO {

     /**
      * Recupera la postazione ('ingresso' o 'uscita') dell'ultimo evento
      * registrato per un dato codice RFID.
      * @param codiceRfid Il codice RFID.
      * @return La postazione ("ingresso" o "uscita") o null se non ci sono eventi.
      */
     private String getLastPostazione(String codiceRfid) {
         String sql = "SELECT t2.postazione " +
                      "FROM timbrature t1 " +
                      "JOIN tornelli t2 ON t1.codice_totem = t2.codice_totem " +
                      "WHERE t1.codice_rfid_utente = ? " +
                      "ORDER BY t1.giorno DESC, t1.ora DESC " +
                      "LIMIT 1";

         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setString(1, codiceRfid);
             ResultSet rs = pstmt.executeQuery();

             if (rs.next()) {
                 return rs.getString("postazione");
             } else {
                 return null; // Nessuna timbratura precedente
             }

         } catch (SQLException e) {
             System.err.println("Errore SQL recupero ultima postazione: " + e.getMessage());
             return "ERROR"; // Indica un errore DB
         }
     }

     /**
      * Recupera la postazione ('ingresso' o 'uscita') di un dato totem.
      * @param codiceTotem Il codice del totem.
      * @return La postazione o null se il totem non esiste.
      */
     private String getTornelloPostazione(String codiceTotem) {
         String sql = "SELECT postazione FROM tornelli WHERE codice_totem = ?";

         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setString(1, codiceTotem);
             ResultSet rs = pstmt.executeQuery();

             if (rs.next()) {
                 return rs.getString("postazione");
             } else {
                 return null; // Totem non trovato
             }

         } catch (SQLException e) {
             System.err.println("Errore SQL recupero postazione totem: " + e.getMessage());
             return "ERROR"; // Indica un errore DB
         }
     }

     /**
      * Controlla se un codice RFID esiste nella tabella lavoratori.
      * @param codiceRfid Il codice RFID da controllare.
      * @return true se esiste, false altrimenti.
      */
      private boolean checkRfidExists(String codiceRfid) {
         String sql = "SELECT id FROM lavoratori WHERE codice_rfid = ?";
         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, codiceRfid);
             ResultSet rs = pstmt.executeQuery();
             return rs.next(); // Ritorna true se trova almeno una riga
         } catch (SQLException e) {
             System.err.println("Errore SQL controllo esistenza RFID: " + e.getMessage());
             return false; // In caso di errore, consideriamo che non esista
         }
      }


     /**
      * Registra un nuovo accesso con controlli di sequenza.
      * @param codiceRfid Il codice RFID del dipendente.
      * @param codiceTotem Il codice del lettore RFID.
      * @return Una stringa che indica il risultato: "OK", "ERR_ALREADY_IN",
      * "ERR_ALREADY_OUT", "ERR_MUST_ENTER", "ERR_RFID_NOT_FOUND",
      * "ERR_TOTEM_NOT_FOUND", "ERR_DB".
      */
     public String registraAccesso(String codiceRfid, String codiceTotem) {

         // 1. Controlla se l'RFID esiste
         if (!checkRfidExists(codiceRfid)) {
            return "ERR_RFID_NOT_FOUND";
         }

         // 2. Ottieni la postazione del totem attuale
         String currentPostazione = getTornelloPostazione(codiceTotem);
         if (currentPostazione == null) {
             return "ERR_TOTEM_NOT_FOUND";
         }
         if ("ERROR".equals(currentPostazione)) {
            return "ERR_DB";
         }

         // 3. Ottieni l'ultima postazione registrata
         String lastPostazione = getLastPostazione(codiceRfid);
         if ("ERROR".equals(lastPostazione)) {
            return "ERR_DB";
         }

         // 4. Applica la logica di controllo
         if (lastPostazione == null) {
             // Se non c'è una timbratura precedente, deve essere un ingresso
             if ("uscita".equals(currentPostazione)) {
                 return "ERR_MUST_ENTER";
             }
         } else {
             // Se c'è una timbratura precedente, non può essere uguale a quella attuale
             if (lastPostazione.equals(currentPostazione)) {
                 return "ingresso".equals(currentPostazione) ? "ERR_ALREADY_IN" : "ERR_ALREADY_OUT";
             }
         }

         // 5. Se tutti i controlli passano, inserisci la nuova timbratura
         String sql = "INSERT INTO timbrature (codice_rfid_utente, codice_totem, giorno, ora) VALUES (?, ?, ?, ?)";
         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setString(1, codiceRfid);
             pstmt.setString(2, codiceTotem);
             pstmt.setDate(3, Date.valueOf(LocalDate.now()));
             pstmt.setTime(4, Time.valueOf(LocalTime.now()));

             int affectedRows = pstmt.executeUpdate();
             return affectedRows > 0 ? "OK" : "ERR_DB"; // Se 0 righe affette, è strano -> ERR_DB

         } catch (SQLException e) {
             System.err.println("Errore SQL durante la registrazione accesso: " + e.getMessage());
             e.printStackTrace();
             return "ERR_DB";
         }
     }

     /**
      * Elimina l'ultimo accesso registrato per un dato codice RFID.
      * @param codiceRfid Il codice RFID del dipendente.
      * @return true se l'eliminazione ha successo, false altrimenti.
      */
     public boolean eliminaUltimoAccesso(String codiceRfid) {
         String findLastIdSql = "SELECT id FROM timbrature WHERE codice_rfid_utente = ? ORDER BY giorno DESC, ora DESC LIMIT 1";
         String deleteSql = "DELETE FROM timbrature WHERE id = ?";
         int lastId = -1;

         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement findStmt = conn.prepareStatement(findLastIdSql)) {

             findStmt.setString(1, codiceRfid);
             ResultSet rs = findStmt.executeQuery();

             if (rs.next()) {
                 lastId = rs.getInt("id");
             } else {
                 System.out.println("Nessuna timbratura trovata per RFID: " + codiceRfid);
                 return false; // Nessuna timbratura trovata
             }

         } catch (SQLException e) {
             System.err.println("Errore SQL durante la ricerca dell'ultimo accesso: " + e.getMessage());
             e.printStackTrace();
             return false;
         }

         // Se abbiamo trovato un ID, procediamo con l'eliminazione
         if (lastId != -1) {
             try (Connection conn = DatabaseManager.getConnection();
                  PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                 deleteStmt.setInt(1, lastId);
                 int affectedRows = deleteStmt.executeUpdate();
                 return affectedRows > 0;

             } catch (SQLException e) {
                 System.err.println("Errore SQL durante l'eliminazione dell'ultimo accesso: " + e.getMessage());
                 e.printStackTrace();
                 return false;
             }
         }
         return false;
     }

     /**
      * Restituisce l'elenco di tutti gli accessi registrati nella giornata odierna.
      * @return Una lista di oggetti Timbratura.
      */
     public List<Timbratura> getAccessiOggi() {
         List<Timbratura> accessi = new ArrayList<>();
         String sql = "SELECT id, codice_rfid_utente, codice_totem, giorno, ora FROM timbrature WHERE giorno = ?";

         try (Connection conn = DatabaseManager.getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setDate(1, Date.valueOf(LocalDate.now()));
             ResultSet rs = pstmt.executeQuery();

             while (rs.next()) {
                 accessi.add(new Timbratura(
                         rs.getInt("id"),
                         rs.getString("codice_rfid_utente"),
                         rs.getString("codice_totem"),
                         rs.getDate("giorno"),
                         rs.getTime("ora")
                 ));
             }

         } catch (SQLException e) {
             System.err.println("Errore SQL durante il recupero degli accessi odierni: " + e.getMessage());
             e.printStackTrace();
         }
         return accessi;
     }
 }
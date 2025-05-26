package com.azienda.timbratura.servlet;

 import com.azienda.timbratura.dao.TimbraturaDAO;
 import com.azienda.timbratura.model.Timbratura;
 // Import Java EE 7 (javax)
 import javax.servlet.ServletException;
 import javax.servlet.annotation.WebServlet;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;

 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.List;
 import java.util.stream.Collectors; // Richiede Java 8+

 @WebServlet("/api/timbrature")
 public class TimbraturaServlet extends HttpServlet {

     private TimbraturaDAO timbraturaDAO;

     @Override
     public void init() throws ServletException {
         timbraturaDAO = new TimbraturaDAO();
     }

     /**
      * Gestisce le richieste GET per ottenere gli accessi odierni.
      */
     @Override
     protected void doGet(HttpServletRequest request, HttpServletResponse response)
             throws ServletException, IOException {

         List<Timbratura> accessi = timbraturaDAO.getAccessiOggi();

         response.setContentType("application/json");
         response.setCharacterEncoding("UTF-8");

         PrintWriter out = response.getWriter();

         String jsonResponse = accessi.stream()
                 .map(Timbratura::toJson)
                 .collect(Collectors.joining(", ", "[", "]"));

         out.print(jsonResponse);
         out.flush();
     }

     /**
      * Gestisce le richieste POST per registrare un nuovo accesso (CON CONTROLLI).
      */
     @Override
     protected void doPost(HttpServletRequest request, HttpServletResponse response)
             throws ServletException, IOException {

         String rfid = request.getParameter("rfid");
         String totem = request.getParameter("totem");

         if (rfid == null || totem == null || rfid.trim().isEmpty() || totem.trim().isEmpty()) {
             response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri 'rfid' e 'totem' sono richiesti.");
             return;
         }

         // Chiama il DAO e ottiene il risultato
         String result = timbraturaDAO.registraAccesso(rfid.trim(), totem.trim());

         response.setCharacterEncoding("UTF-8"); // Imposta encoding per i messaggi

         // Imposta lo status HTTP e il messaggio in base al risultato
         switch (result) {
             case "OK":
                 response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created
                 response.getWriter().println("Accesso registrato con successo.");
                 break;
             case "ERR_ALREADY_IN":
                 response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
                 response.getWriter().println("Errore: Il dipendente è già registrato come ENTRATO.");
                 break;
             case "ERR_ALREADY_OUT":
                 response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
                 response.getWriter().println("Errore: Il dipendente è già registrato come USCITO.");
                 break;
             case "ERR_MUST_ENTER":
                 response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
                 response.getWriter().println("Errore: Il dipendente deve prima registrare un INGRESSO.");
                 break;
            case "ERR_RFID_NOT_FOUND":
                 response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                 response.getWriter().println("Errore: Codice RFID non valido o non associato.");
                 break;
            case "ERR_TOTEM_NOT_FOUND":
                 response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                 response.getWriter().println("Errore: Codice Totem non valido.");
                 break;
             case "ERR_DB":
             default:
                 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                 response.getWriter().println("Errore interno del server durante la registrazione.");
                 break;
         }
         response.getWriter().flush();
     }


     /**
      * Gestisce le richieste DELETE per eliminare l'ultimo accesso di un utente.
      */
     @Override
     protected void doDelete(HttpServletRequest request, HttpServletResponse response)
             throws ServletException, IOException {

         String rfid = request.getParameter("rfid");

         if (rfid == null || rfid.trim().isEmpty()) {
             response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametro 'rfid' è richiesto.");
             return;
         }

         boolean success = timbraturaDAO.eliminaUltimoAccesso(rfid.trim());

         if (success) {
             response.setStatus(HttpServletResponse.SC_OK); // 200 OK
             response.getWriter().println("Ultimo accesso eliminato con successo.");
         } else {
             // Potrebbe essere 404 (non trovato) o 500 (errore). Usiamo 500 per semplicità,
             // ma potremmo aggiungere un controllo nel DAO per 404.
             response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante l'eliminazione o nessun accesso trovato.");
         }
     }
 }
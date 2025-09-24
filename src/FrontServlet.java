import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {  
    /**
     * Traite les requêtes HTTP GET
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        // Configuration de la réponse
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Construction de la réponse HTML
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontServlet - GET</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Test Framework</h1>");
            out.println("<p>Requête GET traitée avec succès</p>");

            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }
    
    /**
     * Traite les requêtes HTTP POST
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Configuration de la réponse
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Construction de la réponse HTML
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontServlet - POST</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>FrontServlet - Méthode POST</h1>");
            out.println("<p>Requête POST traitée avec succès</p>");
            out.println("<p>Paramètres reçus :</p>");
            out.println("<ul>");
            
            // Affichage des paramètres de la requête
            request.getParameterMap().forEach((key, values) -> {
                out.println("<li>" + key + " = " + String.join(", ", values) + "</li>");
            });
            
            out.println("</ul>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }
}

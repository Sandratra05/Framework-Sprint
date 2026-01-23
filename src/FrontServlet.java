
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import annotations.JsonResponse;
import utils.*;
import utils.AuthChecker.AuthResult;
import view.ModelView;



/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 10 * 1024 * 1024L, // 10 MB par fichier
    maxRequestSize = 50 * 1024 * 1024L // 50 MB total
)
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    HashMap<String, List<Scan.MethodInfo>> urlMapping;
    AuthChecker authChecker;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");
            if (input != null) {
                props.load(input);
                String basePackage = props.getProperty("base.package", "controllers");
                this.urlMapping = Scan.getClassesWithAnnotations
                (basePackage);
                String userKey = props.getProperty("session.user.key", "user");
                String roleKey = props.getProperty("session.role.key", "role");
                this.authChecker = new AuthChecker(userKey, roleKey);
            } else {
                // Fallback si le fichier n'existe pas
                this.urlMapping = Scan.getClassesWithAnnotations("controllers");
                this.authChecker = new AuthChecker("user", "role");
            }

            getServletContext().setAttribute("urlMapping", this.urlMapping);
        
        } catch (Exception e) {

        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        /**
         * Example: 
         * If URI is /app/folder/file.html 
         * and context path is /app,
         * then path = /folder/file.html
         */
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        @SuppressWarnings("unchecked")
        HashMap<String, List<Scan.MethodInfo>> urlMaps = (HashMap<String, List<Scan.MethodInfo>>) getServletContext().getAttribute("urlMapping");


        // find matching method (support patterns like /livres/{id})
        Scan.MethodInfo info = Scan.findMatching(urlMaps, path, req.getMethod());
        if (info != null) {
            try {
                Object instance = info.clazz.getDeclaredConstructor().newInstance();

                redirection(instance, info, req, res);

            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = res.getWriter()) {
                    out.println("Error processing request: " + e.getMessage());
                }
            }
        } else {
            try (PrintWriter out = res.getWriter()) {
                String uri = req.getRequestURI();
                String responseBody = """
                    <html>
                        <head><title>Resource Not Found</title></head>
                        <body>
                            <h1>Error 404 - Not Found</h1>
                            <p>Unknown ressource : The requested URL was not found: <strong>%s</strong></p>
                        </body>
                    </html>
                    """.formatted(uri);

                res.setContentType("text/html;charset=UTF-8");
                out.println(responseBody);
            }
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }

    private void redirection (Object instance, Scan.MethodInfo info, HttpServletRequest req, HttpServletResponse res) throws Exception {
        // Vérifier l'autorisation avant de procéder
        if (authChecker != null) {
            // Utiliser getSession(false) pour ne pas créer de session si elle n'existe pas
            HttpSession existingSession = req.getSession(false);
            SessionMap sessionMap = new SessionMap(existingSession);  // SessionMap gère null
            
            AuthResult authResult = authChecker.checkAuthorization(info.method, sessionMap);
            
            if (!authResult.isAllowed()) {
                System.out.println("[FrontServlet] Access DENIED: " + authResult.getErrorCode() + " - " + authResult.getMessage());
                
                // Déterminer le code HTTP approprié
                int httpStatus;
                switch (authResult.getErrorCode()) {
                    case "NOT_AUTHENTICATED":
                    case "NO_ROLE":
                        httpStatus = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                        break;
                    case "INSUFFICIENT_ROLE":
                        httpStatus = HttpServletResponse.SC_FORBIDDEN;  // 403
                        break;
                    default:
                        httpStatus = HttpServletResponse.SC_FORBIDDEN;
                }
                
                res.setStatus(httpStatus);
                res.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = res.getWriter()) {
                    out.println("<!DOCTYPE html>");
                    out.println("<html><head><title>Access Denied</title></head><body>");
                    out.println("<h1>" + httpStatus + " - Access Denied</h1>");
                    out.println("<p><strong>Error:</strong> " + authResult.getErrorCode() + "</p>");
                    out.println("<p>" + authResult.getMessage() + "</p>");
                    out.println("<hr>");
                    out.println("</body></html>");
                }
                return;  // Ne pas invoquer la méthode
            }
        }


        try {
            CheckParameters cp = new CheckParameters();
            Object[] args = cp.checkArgs(info, req);
            
            Object result = info.method.invoke(instance, args);
    
            if (info.method.isAnnotationPresent(JsonResponse.class)) {
                JsonResponse annotation = info.method.getAnnotation(JsonResponse.class);
                String status = annotation.status();
                int code = annotation.code();
            
                String json = JsonUtil.createResponseJson(result, status, code);
                res.setContentType("application/json;charset=UTF-8");
                res.setStatus(code);
            
                try (PrintWriter out = res.getWriter()) {
                    out.println(json);
                }
            } else {

                if (result instanceof String) {

                    res.setContentType("text/html;charset=UTF-8");

                    try (PrintWriter out = res.getWriter()) {
                        out.println("Controller : " + info.clazz.getName() + "<br>");
                        out.println("Method : " + info.method.getName() + "<br>");
                        out.println(result);
                    }
                } else if (result instanceof ModelView) {

                    ModelView mv = (ModelView) result;

                    for (Map.Entry<String, Object> entry : mv.getItems().entrySet()) {
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }

                    RequestDispatcher rd = req.getRequestDispatcher(mv.getView());
                    rd.forward(req, res);
                }
            }
        } catch (Exception e) {
            if (info.method.isAnnotationPresent(JsonResponse.class)) {

                int errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // 500 par défaut

                if (e instanceof IllegalArgumentException) {
                    errorCode = HttpServletResponse.SC_BAD_REQUEST; // 400
                } else if (e instanceof NumberFormatException) {
                    errorCode = HttpServletResponse.SC_BAD_REQUEST;
                }

                String errorJson = JsonUtil.createResponseJson(null, "error", errorCode);

                res.setContentType("application/json;charset=UTF-8");
                res.setStatus(errorCode);

                try (PrintWriter out = res.getWriter()) {
                    out.println(errorJson);
                }
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                try (PrintWriter out = res.getWriter()) {
                    out.println("Error processing request: " + e.getMessage());
                }
            }
        }
    }
    
}


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import utils.*;
import view.ModelView;



/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    HashMap<String, List<Scan.MethodInfo>> urlMapping;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");
            if (input != null) {
                props.load(input);
                String basePackage = props.getProperty("base.package", "controllers");
                this.urlMapping = Scan.getClassesWithAnnotations(basePackage);
            } else {
                // Fallback si le fichier n'existe pas
                this.urlMapping = Scan.getClassesWithAnnotations("controllers");
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
        CheckParameters cp = new CheckParameters();
        Object[] args = cp.checkArgs(info, req);

        Object result = info.method.invoke(instance, args);

        if (result instanceof String) {

            String resultStr = (String) result;
            
            res.setContentType("text/html;charset=UTF-8");
            
            try (PrintWriter out = res.getWriter()) {
                out.println("Controller : " + info.clazz.getName() + "<br>");
                out.println("Method : " + info.method.getName() + "<br>");
                out.println(resultStr);
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

    
}

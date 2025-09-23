// ...existing code...
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet implements Filter {  

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // init si nécessaire
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String context = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(context.length());

        if (path.equals("/front")) {
            chain.doFilter(request, response);
            return;
        }

        req.getRequestDispatcher("/front").forward(req, res);
    }

    @Override
    public void destroy() {
        // cleanup si nécessaire
    }
}
// ...existing code...
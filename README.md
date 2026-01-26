# Ririnina

Ririnina is a lightweight, annotation-driven MVC framework for Java Servlet applications, inspired by the principles of Spring MVC. It simplifies web development by providing a clear structure for handling requests, managing state, and rendering views through a powerful front controller.

## Core Features
- **Front Controller Pattern:** A single `FrontServlet` acts as the entry point for all web requests, routing them to the appropriate controller methods.
- **Annotation-Driven Configuration:** Use intuitive annotations like `@Controller`, `@GetMapping`, and `@PostMapping` to define controllers and map URLs to methods, eliminating the need for complex XML configuration.
- **Flexible Parameter Binding:** Automatically maps HTTP request parameters, path variables (e.g., `/users/{id}`), and multipart file uploads to your controller method arguments. It supports binding to primitive types, custom objects, and file `Part` collections.
- **Versatile Return Types:** Controller methods can return:
    - `ModelView`: To forward the request to a view (like a JSP) with model data.
    - `String`: For direct plain text or HTML output.
    - Any `Object`: Automatically serialized to JSON when the method is annotated with `@JsonResponse`.
- **Integrated JSON Support:** Seamlessly create RESTful APIs by annotating methods with `@JsonResponse`. The framework handles object serialization and wraps the data in a consistent response structure.
- **Simplified Session Management:** Access and modify the `HttpSession` as a standard `Map<String, Object>` by simply annotating a method parameter with `@Session`.
- **Built-in Security:** Secure your endpoints with ease using `@Authorized` to require user authentication and `@Role` to enforce role-based access control.
- **Customizable:** Configure the base package for controllers and session attribute keys via a simple `application.properties` file.

## Getting Started

### 1. Configuration
To use the framework, you must first declare `FrontServlet` in your web application's deployment descriptor (`web.xml`).

```xml
<!-- web.xml -->
<servlet>
    <servlet-name>FrontServlet</servlet-name>
    <servlet-class>FrontServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>FrontServlet</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

### 2. Properties
Create an `application.properties` file in your classpath (e.g., `src/main/resources`) to configure the framework.

```properties
# The base package where the framework will scan for @Controller classes
base.package=com.example.controllers

# (Optional) Session keys used for authentication and authorization
session.user.key=currentUser
session.role.key=userRole
```

## Usage Examples

### Creating a Controller
A controller is a simple Java class annotated with `@Controller`. Methods are mapped to URLs using `@GetMapping` or `@PostMapping`.

```java
import annotations.Controller;
import annotations.GetMapping;
import annotations.RequestParam;
import view.ModelView;

@Controller
public class HomeController {

    // Maps GET /hello to this method
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    // Maps GET /greet?name=John to this method
    @GetMapping("/greet")
    public ModelView greetUser(@RequestParam("name") String userName) {
        ModelView mv = new ModelView();
        mv.setView("/views/greeting.jsp");
        mv.addAttribute("user", userName);
        return mv;
    }
}
```

### Path Variables
The framework supports capturing values from the URL path.

```java
import annotations.Controller;
import annotations.GetMapping;

@Controller
public class UserController {

    // Maps GET /users/123 to this method
    // The value '123' will be passed to the 'userId' parameter.
    @GetMapping("/users/{id}")
    public String getUserById(int id) {
        // Your logic to fetch user by ID
        return "Fetching user with ID: " + id;
    }
}
```

### Returning JSON
Create REST endpoints by annotating a method with `@JsonResponse`. The return object will be serialized to JSON automatically.

```java
import annotations.Controller;
import annotations.GetMapping;
import annotations.JsonResponse;
import com.example.models.Product; // Your custom Product class

@Controller
public class ApiController {

    @GetMapping("/api/products")
    @JsonResponse(status = "success", code = 200)
    public List<Product> getAllProducts() {
        // This list of Product objects will be converted to a JSON array.
        return productService.findAll();
    }
}
```

### File Uploads
Handle file uploads by specifying `Part` or `List<Part>` as a method parameter.

```java
import annotations.Controller;
import annotations.PostMapping;
import jakarta.servlet.http.Part;

@Controller
public class UploadController {

    @PostMapping("/upload/profile-picture")
    public String uploadProfilePicture(@RequestParam("imageFile") Part file) {
        // Logic to save the file
        String fileName = file.getSubmittedFileName();
        long fileSize = file.getSize();
        
        // saveFile(file.getInputStream(), fileName);
        
        return "File " + fileName + " uploaded successfully!";
    }
}
```

### Session Management
Inject the current session as a `Map` using the `@Session` annotation. Any changes made to the map will be reflected in the `HttpSession`.

```java
import annotations.Controller;
import annotations.GetMapping;
import annotations.Session;
import java.util.Map;

@Controller
public class CartController {

    @GetMapping("/cart/add")
    public String addToCart(@RequestParam("productId") int productId, @Session Map<String, Object> session) {
        Integer count = (Integer) session.getOrDefault("itemCount", 0);
        session.put("itemCount", count + 1);
        
        // Add product to a list in the session
        // ...

        return "Item added. Total items in cart: " + session.get("itemCount");
    }
}
```

### Security
Secure endpoints by requiring authentication (`@Authorized`) or a specific role (`@Role`).

```java
import annotations.Authorized;
import annotations.Controller;
import annotations.GetMapping;
import annotations.Role;

@Controller
public class AdminController {

    // Requires the user to be logged in.
    @GetMapping("/dashboard")
    @Authorized
    public String showDashboard() {
        return "Welcome to your dashboard!";
    }

    // Requires the user to be logged in and have the role "ADMIN".
    @GetMapping("/admin/settings")
    @Role("ADMIN")
    public String showAdminSettings() {
        return "Admin settings page.";
    }
}
```

## Annotations Reference
| Annotation       | Target          | Description                                                                                             |
|------------------|-----------------|---------------------------------------------------------------------------------------------------------|
| `@Controller`    | Class           | Marks a class as a web controller.                                                                      |
| `@GetMapping`    | Method          | Maps HTTP GET requests to a specific handler method. Takes a URL pattern as a value.                    |
| `@PostMapping`   | Method          | Maps HTTP POST requests to a specific handler method. Takes a URL pattern as a value.                   |
| `@RequestParam`  | Parameter       | Binds a method parameter to a web request parameter. Can also be used to name parameters or prefixes.   |
| `@JsonResponse`  | Method          | Indicates that the return value should be serialized to JSON and written to the HTTP response body.       |
| `@Session`       | Parameter       | Injects a `Map<String, Object>` wrapper for the `HttpSession` into the method.                          |
| `@Authorized`    | Method          | Restricts access to authenticated users only.                                                           |
| `@Role`          | Method          | Restricts access to authenticated users with a specific role. Implies `@Authorized`.                    |

## Build
To build the framework as a distributable JAR file, execute the `deploy-jar.sh` script. This script compiles the source code and packages it into `frameworkServlet.jar`.

```bash
./deploy-jar.sh
```
You can then include this JAR in the `WEB-INF/lib` directory of your target web application.

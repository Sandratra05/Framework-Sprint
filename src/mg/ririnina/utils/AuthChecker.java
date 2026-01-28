package mg.ririnina.utils;

import java.util.Map;

import mg.ririnina.annotations.Role;
import mg.ririnina.annotations.Authorized;

public class AuthChecker {
    private final String userKey;
    private final String roleKey;

    public AuthChecker(String userKey, String roleKey) {
        this.userKey = userKey;
        this.roleKey = roleKey;
        System.out.println("[AuthChecker] Initialized with userKey='" + userKey + "', roleKey='" + roleKey + "'");
    }

    public String getUserKey() {
        return userKey;
    }

    public String getRoleKey() {
        return roleKey;
    }

    /**
     * Vérifie si l'utilisateur est autorisé pour la méthode annotée.
     * @param method La méthode à vérifier
     * @param session La map de session (peut être null ou vide)
     * @return AuthResult avec le statut et un message d'erreur si refusé
     */
    public AuthResult checkAuthorization(java.lang.reflect.Method method, Map<String, Object> session) {
        // Si pas d'annotation de sécurité, autoriser
        boolean hasAuthorized = method.isAnnotationPresent(Authorized.class);
        boolean hasRole = method.isAnnotationPresent(Role.class);
        
        if (!hasAuthorized && !hasRole) {
            return AuthResult.allowed();  // Pas de restriction
        }

        // Vérifier @Authorized : utilisateur doit être authentifié
        if (hasAuthorized || hasRole) {  // @Role implique aussi une authentification
            Object user = (session != null) ? session.get(userKey) : null;
            if (user == null) {
                return AuthResult.denied(
                    "NOT_AUTHENTICATED",
                    "User not authenticated. Please login first. " +
                    "(Hint: Ensure you store '" + userKey + "' in session during login)"
                );
            }
        }

        // Vérifier @Role : rôle requis
        if (hasRole) {
            Role roleAnnotation = method.getAnnotation(Role.class);
            String requiredRole = roleAnnotation.value();
            Object userRole = (session != null) ? session.get(roleKey) : null;
            
            if (userRole == null) {
                return AuthResult.denied(
                    "NO_ROLE",
                    "User has no role assigned. Required role: '" + requiredRole + "'. " +
                    "(Hint: Ensure you store '" + roleKey + "' in session during login)"
                );
            }
            
            if (!requiredRole.equals(userRole.toString())) {
                return AuthResult.denied(
                    "INSUFFICIENT_ROLE",
                    "Access denied. Required role: '" + requiredRole + "', but user has role: '" + userRole + "'"
                );
            }
        }

        return AuthResult.allowed();
    }

    /**
     * Classe pour encapsuler le résultat de l'autorisation
     */
    public static class AuthResult {
        private final boolean allowed;
        private final String errorCode;
        private final String message;

        private AuthResult(boolean allowed, String errorCode, String message) {
            this.allowed = allowed;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static AuthResult allowed() {
            return new AuthResult(true, null, null);
        }

        public static AuthResult denied(String errorCode, String message) {
            return new AuthResult(false, errorCode, message);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }
}
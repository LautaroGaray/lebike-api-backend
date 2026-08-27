package com.example.scaffold.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    public static final String SESSION_TOKEN = TokenService.class.getName() + ".TOKEN";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE_ID = "roleId";
    private static final String CLAIM_ROLE_NAME = "roleName";

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-minutes:240}")
    private long expirationMinutes;

    /** Tokens invalidados explícitamente (logout). Se limpia al reiniciar, lo cual es seguro. */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public synchronized String getOrCreateToken(HttpSession session) {
        return getOrCreateToken(session, null, null, null);
    }

    public synchronized String getOrCreateToken(HttpSession session, Long roleId, String roleName) {
        return getOrCreateToken(session, null, roleId, roleName);
    }

    public synchronized String getOrCreateToken(HttpSession session, Long userId, Long roleId, String roleName) {
        Object existing = session.getAttribute(SESSION_TOKEN);
        if (existing instanceof String && isValid((String) existing)) {
            // Si ya hay un JWT válido en la sesión, lo reutilizamos
            return (String) existing;
        }

        String token = buildJwt(session.getId(), userId, roleId, roleName);
        session.setAttribute(SESSION_TOKEN, token);
        return token;
    }

    /**
     * Valida el JWT: firma correcta, no expirado y no en blacklist.
     * Es 100% stateless — sobrevive reinicios del servidor.
     */
    public boolean isValid(String token) {
        if (token == null || blacklist.contains(token)) {
            return false;
        }
        try {
            Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getRoleId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            Number roleIdValue = claims.get(CLAIM_ROLE_ID, Number.class);
            return roleIdValue != null ? roleIdValue.longValue() : null;
        } catch (JwtException | IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    public Long getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            Number userIdValue = claims.get(CLAIM_USER_ID, Number.class);
            return userIdValue != null ? userIdValue.longValue() : null;
        } catch (JwtException | IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    public String getRoleName(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get(CLAIM_ROLE_NAME, String.class);
        } catch (JwtException | IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    /** Añade el token a la blacklist (logout explícito). */
    public void invalidateToken(String token) {
        if (token != null) {
            blacklist.add(token);
        }
    }

    /**
     * Mantiene compatibilidad con SessionLifecycleListener.
     * La sesión ya tiene el JWT como atributo; úsala directamente.
     * @deprecated Usa {@link #invalidateToken(String)} desde el listener.
     */
    @Deprecated
    public void invalidateSession(String sessionId) {
        // No-op: JWT es stateless, la invalidación real se hace con invalidateToken()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildJwt(String sessionId, Long userId, Long roleId, String roleName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000L);

        return Jwts.builder()
                .setSubject(sessionId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE_ID, roleId)
                .claim(CLAIM_ROLE_NAME, roleName)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }
}

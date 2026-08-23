package com.example.scaffold.security;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionLifecycleListener implements HttpSessionListener {
    private final TokenService tokenService;

    public SessionLifecycleListener(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Object token = session.getAttribute(TokenService.SESSION_TOKEN);
        if (token instanceof String) {
            tokenService.invalidateToken((String) token);
        }
    }
}

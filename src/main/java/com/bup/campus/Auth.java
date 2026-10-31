package com.bup.campus;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Tells which user is signed in, based on the server-side session (not on what the browser sends). */
public final class Auth {

    public static final String KEY = "universityId";

    private Auth() {}

    public static String id() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpSession session = sra.getRequest().getSession(false);
            if (session != null && session.getAttribute(KEY) instanceof String id) {
                return id;
            }
        }
        return null;
    }
}
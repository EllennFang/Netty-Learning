package com.fang.server.session;

public abstract class SessionFactory {

    private static com.fang.server.session.Session session = new com.fang.server.session.SessionMemoryImpl();

    public static com.fang.server.session.Session getSession() {
        return session;
    }
}

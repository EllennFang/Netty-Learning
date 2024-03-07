package com.fang.server.service;

public abstract class UserServiceFactory {

    private static com.fang.server.service.UserService userService = new UserServiceMemoryImpl();

    public static com.fang.server.service.UserService getUserService() {
        return userService;
    }
}

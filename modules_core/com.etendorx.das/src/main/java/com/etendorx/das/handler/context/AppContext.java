package com.etendorx.das.handler.context;

public class AppContext {
    private static final ThreadLocal<UserContext> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(UserContext userContext) {
        currentUser.set(userContext);
    }

    public static UserContext getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}

package com.swifttrack.app.core.network;

public class Resource<T> {

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING,
        OFFLINE
    }

    public final Status status;
    public final T data;
    public final String message;
    public final String errorCode;

    private Resource(Status status, T data, String message, String errorCode) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null, null);
    }

    public static <T> Resource<T> error(String msg, String code, T data) {
        return new Resource<>(Status.ERROR, data, msg, code);
    }

    public static <T> Resource<T> loading(T data) {
        return new Resource<>(Status.LOADING, data, null, null);
    }

    public static <T> Resource<T> offline(T data, String msg) {
        return new Resource<>(Status.OFFLINE, data, msg, "OFFLINE_CACHE");
    }
}

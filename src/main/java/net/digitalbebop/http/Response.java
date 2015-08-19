package net.digitalbebop.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;

public final class Response {

    private static final ProtocolVersion VERSION = HttpVersion.HTTP_1_1;
    private static final HttpResponse okResponse = new BasicHttpResponse(VERSION, HttpStatus.SC_OK, "OK");
    private static final HttpResponse notFoundResponse = new BasicHttpResponse(VERSION, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    private static final HttpResponse serverErrorResponse = new BasicHttpResponse(VERSION, HttpStatus.SC_INTERNAL_SERVER_ERROR, "server error");

    public static HttpResponse badRequest(String message) {
        return new BasicHttpResponse(VERSION, HttpStatus.SC_BAD_REQUEST, message);
    }

    public static HttpResponse notFound() {
        return notFoundResponse;
    }

    public static HttpResponse ok() {
        return okResponse;
    }

    public static HttpResponse ok(byte[] payload) {
        HttpResponse response = new BasicHttpResponse(VERSION, HttpStatus.SC_OK, "OK");
        response.setEntity(new ByteArrayEntity(payload));
        return response;
    }

    public static HttpResponse serverError(String message) {
        return new BasicHttpResponse(VERSION, HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
    }

    public static HttpResponse serverError() {
        return serverErrorResponse;
    }

}
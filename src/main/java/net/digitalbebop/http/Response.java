package net.digitalbebop.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;

import java.io.InputStream;
import java.io.OutputStream;

public final class Response {
    public static final ProtocolVersion VERSION = HttpVersion.HTTP_1_1;
    public static final HttpResponse ok = new BasicHttpResponse(VERSION, HttpStatus.SC_OK, "OK");
    public static final HttpResponse notFound = new BasicHttpResponse(VERSION, HttpStatus.SC_NOT_FOUND, "NOT IMPLEMENTED");
    public static final HttpResponse serverError = new BasicHttpResponse(VERSION, HttpStatus.SC_INTERNAL_SERVER_ERROR, "server error");
    public static final HttpResponse badProtobuf = new BasicHttpResponse(VERSION, HttpStatus.SC_BAD_REQUEST, "invalid protocol buffer");
    public static final HttpResponse noAuth = new BasicHttpResponse(VERSION, HttpStatus.SC_FORBIDDEN, "forbidden request");

    public static HttpResponse badRequest(String message) {
        return new BasicHttpResponse(VERSION, HttpStatus.SC_BAD_REQUEST, message);
    }

    public static HttpResponse ok(byte[] payload) {
        HttpResponse response = new BasicHttpResponse(VERSION, HttpStatus.SC_OK, "OK");
        response.addHeader(new BasicHeader("Access-Control-Allow-Origin", "*"));
        response.setEntity(new ByteArrayEntity(payload));
        return response;
    }

    public static HttpResponse serverError(String message) {
        return new BasicHttpResponse(VERSION, HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
    }
}
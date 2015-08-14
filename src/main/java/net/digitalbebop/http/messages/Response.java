package net.digitalbebop.http.messages;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;

public abstract class Response {
    protected int status;
    protected String message;
    protected byte[] payload;

    public Response(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(int status, String message, byte[] payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    public HttpResponse getHttpResponse() {
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, message);
        if (payload != null) {
            response.setEntity(new ByteArrayEntity(payload));
        }
        return response;
    }
}

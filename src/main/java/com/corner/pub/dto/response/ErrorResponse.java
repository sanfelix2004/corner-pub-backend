package com.corner.pub.dto.response;

public class ErrorResponse {
    private String error;

    private String code;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String code, String error) {
        this.code = code;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

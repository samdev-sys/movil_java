package com.icass.chatfirebase.retrofit.services;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ConnectionService {
    @GET("/posts/1")
    Call<String> request();
}
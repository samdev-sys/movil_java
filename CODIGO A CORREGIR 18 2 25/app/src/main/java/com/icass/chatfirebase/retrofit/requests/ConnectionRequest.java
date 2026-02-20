package com.icass.chatfirebase.retrofit.requests;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.retrofit.NetworkListener;
import com.icass.chatfirebase.retrofit.services.ConnectionService;
import com.icass.chatfirebase.utils.LogUtils;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ConnectionRequest {
    private static final String TAG = ConnectionRequest.class.getSimpleName();
    @NonNull
    private final NetworkListener listener;

    private static final int NUMBER_OF_THREADS = 4;

    @NonNull
    private final Executor networkThread = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    @NonNull
    private Executor networkIO() {
        return networkThread;
    }

    public ConnectionRequest(@NonNull NetworkListener listener) {
        this.listener = listener;
    }

    public void execute() {
        final String BASE_URL = "https://jsonplaceholder.typicode.com";
        final OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .callTimeout(10L, TimeUnit.SECONDS)
                .connectTimeout(10L, TimeUnit.SECONDS)
                .readTimeout(10L, TimeUnit.SECONDS)
                .writeTimeout(10L, TimeUnit.SECONDS);

        final Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create());

        builder.client(httpClient.build());

        final Retrofit retrofit = builder.build();

        networkIO().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //Create service
                    final ConnectionService apiService = retrofit.create(ConnectionService.class);

                    final Response<String> response = apiService.request().execute();
                    if(response.isSuccessful()) {
                        listener.onConnected();
                        LogUtils.d("ConectionRequest", "JSON: " + response.body());
                    } else {
                        listener.onDisconnected();
                    }
                } catch (IOException ex) {
                    listener.onDisconnected();
                    LogUtils.e(TAG,"Error: " + ex);
                }
            }
        });
    }
}
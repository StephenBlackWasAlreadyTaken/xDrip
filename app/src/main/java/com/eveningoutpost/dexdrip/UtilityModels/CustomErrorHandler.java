package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.RestErrors;
import com.eveningoutpost.dexdrip.R;
import com.google.gson.Gson;

import org.json.JSONObject;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 12/12/14.
 */
public class CustomErrorHandler implements ErrorHandler {
//    private final Context ctx;

    public CustomErrorHandler() {
//        this.ctx = ctx;
    }

    @Override
    public Throwable handleError(RetrofitError cause) {
        String errorDescription;

        if (cause.isNetworkError()) {
            errorDescription = "Network Error, try again";
        } else {
            if (cause.getResponse() == null) {
                errorDescription = "No Response from server, try again";
            } else {

                // Error message handling - return a simple error to Retrofit handlers..
                try {
                    RestErrors errorResponse = (RestErrors) cause.getBodyAs(RestErrors.class);
//                    errorDescription = errorResponse.error.data.message;
                    errorDescription = errorResponse.error.message;
                } catch (Exception ex) {
                    try {
                        errorDescription = String.valueOf(cause.getResponse().getStatus());
                    } catch (Exception ex2) {
                        Log.e("ERROR HANDLER: ", "handleError: " + ex2.getLocalizedMessage());
                        errorDescription = "Unknown Error";
                    }
                }
            }
        }
        return new Exception(errorDescription);
    }
}

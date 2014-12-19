package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Interfaces.UserInterface;
import com.eveningoutpost.dexdrip.UtilityModels.CustomErrorHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.bind.DateTypeAdapter;

import org.json.JSONObject;

import java.util.Date;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

/**
 * Created by stephenblack on 11/7/14.
 */
@Table(name = "User", id = BaseColumns._ID)
public class User extends Model {
    private static final String baseUrl = "http://10.0.2.2:3000";

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .create();

    @Expose
    @Column(name = "email")
    public String email;

    @Expose
    @Column(name = "password")
    public String password;

    @Expose
    @Column(name = "token")
    public String token;

    @Expose
    @Column(name = "token_expiration")
    public double token_expiration;

    @Column(name = "secret_key")
    public String secret_key;

    @Expose
    @Column(name = "seed")
    public String seed;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;


    public static User currentUser() {
        User user = new Select()
                .from(User.class)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return user;
    }

    public String register() {
        try {
            User userResponse = userInterface().register(this);

            token = userResponse.token;
            token_expiration = userResponse.token_expiration;
            seed = userResponse.seed;
            save();

            Log.w("REST CALL SUCCESS:", " **************** " + email);
            Log.w("REST CALL SUCCESS:", " **************** " + secret_key);
            Log.w("REST CALL SUCCESS:", " **************** " + password);
            Log.w("REST CALL SUCCESS:", " **************** " + token_expiration);
            return "Success";
        } catch (RetrofitError e) {
            try {
                RestErrors errorResponse = (RestErrors) e.getBodyAs(RestErrors.class);
                return errorResponse.error.message;
            } catch (Exception ex) {
                try {
                    return String.valueOf(e.getResponse().getStatus());
                } catch (Exception ex2) {
                    Log.e("ERROR HANDLER: ", "handleError: " + ex2.getLocalizedMessage());
                    return "Unknown Error";
                }
            }
        }
    }

    public String authenticate() {
        try {
            User userResponse = userInterface().authenticate(this);
            Log.w("REST CALL SUCCESS:", " **************** " + userResponse.email);
            Log.w("REST CALL SUCCESS:", " **************** " + userResponse.secret_key);
            Log.w("REST CALL SUCCESS:", " **************** " + userResponse.password);
            Log.w("REST CALL SUCCESS:", " **************** " + userResponse.token_expiration);
            token = userResponse.token;
            token_expiration = userResponse.token_expiration;
            seed = userResponse.seed;
            save();
            return "Success";
        } catch (RetrofitError e) {
            try {
                RestErrors errorResponse = (RestErrors) e.getBodyAs(RestErrors.class);
                Log.w("REST CALL FAILURE:", " **************** " + errorResponse.error.message);
                Log.w("REST CALL FAILURE:", " **************** " + errorResponse.toString());
                return errorResponse.error.message;
            } catch (Exception ex) {
                try {
                    return String.valueOf(e.getResponse().getStatus());
                } catch (Exception ex2) {
                    Log.e("ERROR HANDLER: ", "handleError: " + ex2.getLocalizedMessage());
                    return "Unknown Error";
                }
            }
        }
    }
    public static UserInterface userInterface() {
        RestAdapter adapter = adapterBuilder().build();
        return adapter.create(UserInterface.class);
    }

    public static RestAdapter.Builder adapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setEndpoint(baseUrl)
                .setConverter(new GsonConverter(gson));
        return adapterBuilder;
    }


}


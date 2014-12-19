package com.eveningoutpost.dexdrip.Interfaces;

import com.eveningoutpost.dexdrip.Models.User;

import org.json.JSONObject;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by stephenblack on 11/10/14.
 */
public interface UserInterface {

    @POST("/api/v1/sessions")
    User authenticate(@Body User user);

    @POST("/api/v1/registrations")
    User register(@Body User user);
}

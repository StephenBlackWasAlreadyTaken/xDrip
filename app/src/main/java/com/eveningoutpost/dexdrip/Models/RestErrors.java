package com.eveningoutpost.dexdrip.Models;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by stephenblack on 12/12/14.
 */
public class RestErrors {

    @Expose
    public Error error;

    public static class Error {

        @Expose
        public String message;
    }
}
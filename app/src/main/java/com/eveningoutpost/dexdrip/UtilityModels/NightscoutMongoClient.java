package com.eveningoutpost.dexdrip.UtilityModels;

import android.util.Log;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.net.UnknownHostException;

/**
 * Created by radu iliescu on 2/1/2015.
 */
public class NightscoutMongoClient {
    private static final String TAG = NightscoutMongoClient.class.getSimpleName();
    private String dbURI;
    private MongoClient client;
    private DB db;
    private static NightscoutMongoClient singleton = null;

    NightscoutMongoClient(String URI) throws UnknownHostException {
        dbURI = URI;
        MongoClientURI uri = new MongoClientURI(dbURI.trim());
        client = new MongoClient(uri);
        db = client.getDB(uri.getDatabase());
    }

    public synchronized static DB nightscoutGetDB(String URI) throws UnknownHostException{
        if (singleton == null) {
            singleton = new NightscoutMongoClient(URI);
        }

        if (!singleton.dbURI.equalsIgnoreCase(URI)) {
            singleton.client.close();
            singleton = new NightscoutMongoClient(URI);
        }

        return singleton.db;
    }

    /* Call this on exception cases so that next nightscoutGetDB
     call would try again connecting and creating the DB */
    public static void resetMongoConnection() {
        Log.i(TAG, "reset connection to mongo");
        if (singleton == null)
            return;
        singleton.client.close();
        singleton.dbURI = "";
    }
}

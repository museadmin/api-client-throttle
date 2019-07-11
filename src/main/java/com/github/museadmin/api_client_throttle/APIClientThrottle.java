package com.github.museadmin.api_client_throttle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The core throttle for the API clients
 * Sets the rate based on original config and then
 * calculated on number of throttle reports it receives
 * from the clients.
 *
 * Effectively this object adds latency to the outgoing API
 * request but at the application level, rather than network level
 * like the Network Link Conditioner for MAC does.
 *
 * Throttle should sleep or loop for period of latency
 */
public class APIClientThrottle {

    // List of requestors in Q
    private ArrayList<APIClientRequestor> requestor = new ArrayList<>();
    private Integer latency;

    public APIClientThrottle() {

    }


}

# api-client-throttle

A utility to apply a configurable or dynamic latency to a client making multiple API calls 
that are subject to rate limiting, such as the AWS CLI.

Intention is that the client should detect that it is being rate limited and advise the throttler
which will then dynamically add a latency to all calls going out from all threads. In this fashion the 
client is able to throttle its own rate of API hits such that it is no longer limited by the target API server, 
but rather by itself.

Current approaches to handling API rate limiting by a service is for the client to implement an incremental backoff and retry strategy.
This generally exacerbates the problem rather than resolves it because the client keeps hitting the API
when it is busy. In the case of AWS – which draws no distinction between its own objects hitting the API and a customer's client application
– this can cause the platform to fail.

By allowing a multi-threaded client application to throttle all threads and ensure synchronous hits on the API at a time, the client can 
throttle back just enough to ensure that the rate limiting does not occur.
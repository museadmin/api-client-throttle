# api-client-throttle

A utility (POJO) to apply a configurable or dynamic latency to a client making multiple API calls
that is subject to rate limiting, such as scanning with the AWS CLI.

Supports multi-threaded applications.

Current approaches to handling API rate limiting by a service is for the client to implement an incremental backoff and retry strategy.
This generally exacerbates the problem rather than resolves it because the client keeps hitting the API when it is busy. In the case of
AWS – which draws no distinction between its own objects hitting the API and a customer's client application
– this can cause the platform to fail.

## Responsive rate limiting
Intention is that the client should detect that it is being rate limited and advise the throttler
which will then dynamically add a latency to all calls going out from all threads. In this fashion the
client is able to throttle its own rate of API hits such that it is no longer limited by the target API server,
but rather by itself.

By allowing a multi-threaded client application to throttle all threads and ensure synchronous hits on the API, the client can
throttle back just enough to ensure that the rate limiting does not occur.

Alternatively, applications can throttle up from a slow rate to a higher rate until it hits rate limiting.

## Static rate limiting
The throttle can be configured with a fixed latency in milliseconds, and does not have to be used for responsive auto-adjusting
applications. For example, in the development environment used, 200ms between API calls was found to be sufficient to avoid
rate limiting even at busy times.

So if you're not hell bent on squeezing every millisecond of performance from your application, you can simply set a fixed
latency that you know will not cause you rate limiting issues.
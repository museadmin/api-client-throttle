package com.github.museadmin.api_client_throttle;

/**
 * Requestor handles making a request to the APIClientThrottle
 * for a place in the queue for API requests.
 */
public class APIClientRequestor {

  // ================= variables =================
  public Boolean inhibited = true;
  public Boolean joinQueue = false;

  // ================= methods =================

  /**
   * Throttle actions the request to joinQueue the queue
   * and resets the control properties
   */
  public void acknowledgeRequest() {
    this.inhibited = true;
    this.joinQueue = false;
  }
  /**
   * Reached head of queue and now dis-inhibited
   */
  public void deQueue() {
    this.inhibited = false;
    this.joinQueue = false;
  }
  /**
   * Application is asking to join the API queue. typically
   * it would prepare an API call and then loop while
   * requestor.inhibited is true.
   */
  public void enQueue() {
    this.inhibited = true;
    this.joinQueue = true;
  }
}

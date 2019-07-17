package com.github.museadmin.api_client_throttle;

import static java.lang.Thread.sleep;

/**
 * Requestor handles making a request to the APIClientThrottle
 * for a place in the queue for API requests.
 */
public class APIClientRequestor {



  // ================= variables =================

  Boolean inhibited = true;
  public Boolean getInhibited() {
    return inhibited;
  }
  Boolean joinQueue = false;

  public Integer getPause() {
    return pause;
  }
  public void setPause(Integer pause) {
    this.pause = pause;
  }
  private Integer pause = 10;

  // ================= methods =================

  /**
   * Throttle actions the request to joinQueue the queue
   * and resets the control properties
   */
  public void acknowledgeRequest() {
    inhibited = true;
    joinQueue = false;
  }
  /**
   * Reached head of queue and now dis-inhibited
   */
  public void deQueue() {
    inhibited = false;
    joinQueue = false;
  }
  /**
   * Application is asking to join the API queue. typically
   * it would prepare an API call and then loop while
   * requestor.inhibited is true.
   */
  public void enQueue() {
    inhibited = true;
    joinQueue = true;
  }

  /**
   * Perform the actual wait for requestors appearance at head of Q
   * Sleep is configurable via get and set pause functions
   * @throws InterruptedException
   */
  public void waitInQ() throws InterruptedException {
    while (inhibited) {
      sleep(pause);
    }
  }
}

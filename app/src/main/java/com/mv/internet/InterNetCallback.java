package com.mv.internet;



/**
 * Created by admin on 2019/1/21.
 */

public interface InterNetCallback {

    void heart(String address);
    void receiveRequest(Object object);
    void findNewUser(String message);
    void removeUser(String message);
    void negotiate(String message);

}
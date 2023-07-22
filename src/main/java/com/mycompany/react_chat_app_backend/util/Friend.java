/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;

/**
 * Friends to the current user.
 * 
 * Used in the GetFriendsList.java
 * 
 * @author chris
 */
public class Friend {
    protected String friend;
    protected Integer friendRequestId;

    public Friend() {
        
    }

    public Friend( String friend, Integer friendRequestId) {
        this.friend = friend;
        this.friendRequestId = friendRequestId;
    }

    /**
     * @return the friend
     */
    public String getFriend() {
        return friend;
    }

    /**
     * @param friend the friend to set
     */
    public void setFriend(String friend) {
        this.friend = friend;
    }

    /**
     * @return the friendRequestId
     */
    public Integer getFriendRequestId() {
        return friendRequestId;
    }

    /**
     * @param friendRequestId the friendRequestId to set
     */
    public void setFriendRequestId(Integer friendRequestId) {
        this.friendRequestId = friendRequestId;
    }
}

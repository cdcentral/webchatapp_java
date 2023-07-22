/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;

/**
 *
 * @author chris
 */
public class FriendAvatarsV2 {

    private Object avatarImage;
    private String avatarImageString;
    private String friend;
    private Integer friendRequestId;
    
    public FriendAvatarsV2() {

    }
    /**
     * @return the avatarImage
     */
    public Object getAvatarImage() {
        return avatarImage;
    }

    /**
     * @param avatarImage the avatarImage to set
     */
    public void setAvatarImage(Object avatarImage) {
        System.out.println(">>>>>>> avatar Image: " + avatarImage.getClass()); // java.io.ByteArrayInputStream
        this.avatarImage = avatarImage; //avatarImage	ByteArrayInputStream	#607	
    }

    /**
     * @return the avatarImageString
     */
    public String getAvatarImageString() {
        return avatarImageString;
    }

    /**
     * @param avatarImageString the avatarImageString to set
     */
    public void setAvatarImageString(String avatarImageString) {
        this.avatarImageString = avatarImageString;
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

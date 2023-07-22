/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;

import java.io.ByteArrayInputStream;

/**
 *
 * @author chris
 */
public class FriendAvatars extends Friend {


    private Object avatarImage;
    private String avatarImageString;

    public FriendAvatars() {

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
}

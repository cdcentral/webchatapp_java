/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;

import java.sql.Timestamp;

/**
 * helper class to extract the latest messages for a chat group.
 * 
 * This is used to help populate the front end chat messages.
 * 
 * @author chris
 */
public class MessageData {

    private String msg_id;
    private String message;
    private Timestamp msg_timestamp;
    private String user_id;
    private String user_name;
    private String chat_group;

    public MessageData(String msg_id, String message, Timestamp msg_timestamp, String user_id, String user_name, String chat_group) {
        this.msg_id = msg_id;
        this.message = message;
        this.msg_timestamp = msg_timestamp;
        this.user_id = user_id;
        this.user_name = user_name;
        this.chat_group = chat_group;
    }

    /**
     * @return the msg_id
     */
    public String getMsg_id() {
        return msg_id;
    }

    /**
     * @param msg_id the msg_id to set
     */
    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the msg_timestamp
     */
    public Timestamp getMsg_timestamp() {
        return msg_timestamp;
    }

    /**
     * @param msg_timestamp the msg_timestamp to set
     */
    public void setMsg_timestamp(Timestamp msg_timestamp) {
        this.msg_timestamp = msg_timestamp;
    }

    /**
     * @return the user_id
     */
    public String getUser_id() {
        return user_id;
    }

    /**
     * @param user_id the user_id to set
     */
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    /**
     * @return the user_name
     */
    public String getUser_name() {
        return user_name;
    }

    /**
     * @param user_name the user_name to set
     */
    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    /**
     * @return the chat_group
     */
    public String getChat_group() {
        return chat_group;
    }

    /**
     * @param chat_group the chat_group to set
     */
    public void setChat_group(String chat_group) {
        this.chat_group = chat_group;
    }
}

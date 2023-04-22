/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;

/**
 * Class representing the chat groups which include their group id and group members.
 * 
 * @author chris
 */
public class ChatGroups {


    /**
     * Specific ID for a chat group.
     */
    private Integer groupID = -1;

    /**
     * Users/Emails that belong to the chat group.
     */
    private String groupMembers = "";
   
    /**
     * Constructor.
     * @param groupID
     * @param groupMembers 
     */
    public ChatGroups(Integer groupID, String groupMembers) {
        this.groupID = groupID;
        this.groupMembers = groupMembers;
    }

    /**
     * @return the groupID
     */
    public Integer getGroupID() {
        return groupID;
    }

    /**
     * @param groupID the groupID to set
     */
    public void setGroupID(Integer groupID) {
        this.groupID = groupID;
    }

    /**
     * @return the groupMembers
     */
    public String getGroupMembers() {
        return groupMembers;
    }

    /**
     * @param groupMembers the groupMembers to set
     */
    public void setGroupMembers(String groupMembers) {
        this.groupMembers = groupMembers;
    }
}

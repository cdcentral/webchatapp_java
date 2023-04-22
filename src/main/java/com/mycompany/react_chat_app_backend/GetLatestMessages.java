/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.mycompany.react_chat_app_backend.util.MessageData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.json.JSONObject;

/**
 * This will receive periodic requests per each user to get the latest messages
 * for the 'chat group' that they currently have selected.
 * 
 * @author chris
 */
public class GetLatestMessages extends HttpServlet {

    /**
     * Class Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GetLatestMessages.class.getName());

    /**
     * These variables will contain the values passed from the react frontend.
     */
    private String chatGroupMembers="";
    private Integer chatGroupID = -1;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        JSONObject jsonResponse = new JSONObject(); 
        extractChatGroup(request);
        ArrayList<MessageData> messages = null;
        
        if (chatGroupMembers.isEmpty() || chatGroupID == -1) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("additionalInfo", "chat group received is blank.");
            jsonResponse.put("messages", messages);
        } else {
            messages = getMessages();
            jsonResponse.put("status", "success");
            jsonResponse.put("additionalInfo", "message size is " + messages.size());
            jsonResponse.put("messages", messages);
        }

        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Gets the latest messages.
     * @return 
     */
    private ArrayList<MessageData>  getMessages() {
        // will contain message data from the table
        JsonArray messagesArray = null;

        // used to parse the data
        Gson gson = new GsonBuilder().create();
        
        ArrayList<MessageData> dataArray = new ArrayList<>();
        Connection conn = null;

        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            String queryOrderByLimit = "select * from group_messages where chat_group_id = ? ORDER BY message_id DESC LIMIT 4";            

            PreparedStatement pstmt = conn.prepareStatement(queryOrderByLimit);
            pstmt.setInt(1, chatGroupID);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {

                MessageData mData = new MessageData(rs.getString("message_id"), rs.getString("message"), rs.getTimestamp("message_timestamp"), rs.getString("user_id_that_posted_msg"), rs.getString("user_name_that_posted_msg"), rs.getString("chat_group"));
                dataArray.add(mData);
            }
            Collections.reverse(dataArray);
            messagesArray = gson.toJsonTree(dataArray).getAsJsonArray();
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception in the GetLatestMessages: {0}", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Exception in the GetLatestMessages: {0}", ex);
                }
                conn = null;
            }
        }
        return dataArray;
    }
    /**
     * Get the chat group from the request.  This will be the key to get the latest
     * messages in the group chat.
     * @param request HttpServletRequest
     * @return 
     */
    private void extractChatGroup(HttpServletRequest request) {
        JSONObject jsonData = null;
        /*
            Read data from the requests buffered reader
        */
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("chatGroupMembers")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    chatGroupMembers = line.trim();
                } else if (line.contains("chatGroupID")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    chatGroupID = Integer.parseInt(line.trim());
                }
            }

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

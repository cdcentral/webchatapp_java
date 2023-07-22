/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.json.JSONObject;

/**
 * This servlet receives messages from the front end to post a message.
 * 
 * This is a message that the user sends for a specific chat group.
 * 
 * @author chris
 */
public class PostMessageServlet extends HttpServlet {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PostMessageServlet.class.getSimpleName());

    /**
     * These variables below will contain the values sent from the react front end.
     */
    private String userMessage = "";
    private String keycloakUserId = "";
    private String keycloakUsername = "";
    private String keycloakUserEmail = "";
    private String chatGroupMembers = "";
    private Integer chatGroupID = -1;

    /**
     * These are fields that we'll need to parse the values for, from the http request
     * received from the front end.
     */
    private final String USER_MESSAGE = "userMessageToPost";
    private final String KEYCLOAK_USER_ID = "keycloakUserId";
    private final String KEYCLOAK_USERNAME = "keycloakUserName";
    private final String KEYCLOAK_USER_EMAIL = "keycloakUserEmail";
    private final String CHAT_GROUP_MEMBERS = "chatGroupMembers";
    private final String CHAT_GROUP_ID = "chatGroupID";

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
        if ( extractDataFromRequest(request)) {
            boolean successful = postMessage();
            jsonResponse.append("status", (successful ? "success" : "failed"));
            jsonResponse.append("message", "message posted");            
        } else {
            jsonResponse.append("status", "failed");
            jsonResponse.append("message", "could not extract fields from http servlet request object.");
        }

        try ( PrintWriter out = response.getWriter()) {
            LOGGER.log(Level.INFO, "Message to be sent back: {0}", jsonResponse.toString());
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Posts the message to the database.
     * @return 
     */
    private boolean postMessage() {
        boolean status = false;
        Integer rowsEffected = -1;
        Connection conn = null;
       
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            /*message_id, chat_group, user_id_that_posted_msg, user_name_that_posted_msg, message */
            PreparedStatement pstmt = 
                    conn.prepareStatement(
                            "INSERT INTO group_messages (chat_group, chat_group_id, user_id_that_posted_msg, user_name_that_posted_msg, user_email_that_posted_msg, message, message_timestamp) VALUES (?,?,?,?,?,?,?);"
                    );
            pstmt.setString(1, chatGroupMembers);
            pstmt.setInt(2, chatGroupID);
            pstmt.setString(3, keycloakUserId);
            pstmt.setString(4, keycloakUsername);
            pstmt.setString(5, keycloakUserEmail);
            pstmt.setString(6, userMessage);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            pstmt.setTimestamp(7, timestamp);
            rowsEffected = pstmt.executeUpdate();
            status = (rowsEffected > 0);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while posting message: {0}", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                conn = null;
            }
        }
        return status;
    }

    /**
     * Extract posted data from react app.
     * @param request HttpServletRequest
     * @return true if the method could extract all the fields and the fields
     * are not empty.  False otherwise.
     */
    private boolean extractDataFromRequest(HttpServletRequest request) {
        boolean dataExtracted = false;
        /*
            Read data from the requests buffered reader
        */
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains(USER_MESSAGE)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userMessage = line.trim();
                } else if (line.contains(KEYCLOAK_USER_ID)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakUserId = line.trim();
                } else if (line.contains(KEYCLOAK_USERNAME)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakUsername = line.trim();
                } else if (line.contains(KEYCLOAK_USER_EMAIL)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakUserEmail = line.trim();
                } else if (line.contains(CHAT_GROUP_MEMBERS)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    chatGroupMembers = line.trim();
                } else if (line.contains(CHAT_GROUP_ID)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    chatGroupID = Integer.parseInt(line.trim());
                }
            }
            LOGGER.log(Level.INFO, "Message received: {0}", msg);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        dataExtracted = (!userMessage.isEmpty() && !chatGroupMembers.isEmpty() && chatGroupID != -1 
                && !keycloakUserId.isEmpty() && !keycloakUsername.isEmpty());
        return dataExtracted;
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

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import com.mycompany.react_chat_app_backend.util.Friend;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
 * This will receive periodic requests per each users session to get the users
 * current friends list.
 * 
 * The user can only make chat groups with people who are part of their friends list.
 * 
 * @author chris
 */
public class GetFriendsList extends HttpServlet {

    private String userKeycloakId = "";
    private String sourceOfRequest = "";
    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GetFriendsList.class.getName());
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
        userKeycloakId = "";
        sourceOfRequest= "";
        getRequestData(request);
        JSONObject jsonResponse = new JSONObject();

        if (sourceOfRequest == null || sourceOfRequest.isEmpty()) {
            // request came from ChatPage component
            ArrayList<String> friends = getFriendsList(userKeycloakId);
            jsonResponse.put("friends", friends);

        } else {
            // request from Settings component.
            // get friend data plus some unique id (to put in react component unique key list)
            ArrayList<Friend> friends = getFriendsWithExtraDataList(userKeycloakId);
            jsonResponse.put("friends", friends);
        }
        try ( PrintWriter out = response.getWriter()) {

            out.println(jsonResponse.toString());
        }
    }

    /**
     * Gets friends list of the user with 'userId'
     * 
     * This will be used by GUI app to show list of friends that the user can
     * initiate a chat with.
     * 
     * @param userId String
     * @return ArrayList<String>
     */
    private ArrayList<String> getFriendsList (String userId) {
        ArrayList<String> friendsList = new ArrayList<String>();
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // First check if the user request already exists.  THIS is to prevent duplicate entries into the table.
            PreparedStatement friendsStatement = conn.prepareStatement("select * from invite where accepted = true and requestor = ?");
            friendsStatement.setString(1, userId);

            ResultSet rows = friendsStatement.executeQuery();
            while (rows.next()) {
                String requestee = rows.getString("requestee");
                friendsList.add(requestee);
                LOGGER.log(Level.INFO, "[GetFriendsList] friend found: {0}", requestee);
            }
        } catch (Exception ex) {
            System.out.println(ex);
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
        return friendsList;
    }

    /**
     * Gets list of friends associated to the requestor (defined by the userId).
     * 
     * The returned data will contain the friend plus their unique requestId.
     * 
     * @param userId String.
     * @return 
     */
    private ArrayList<Friend> getFriendsWithExtraDataList (String userId) {
        ArrayList<Friend> friendsList = new ArrayList<Friend>();
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();

            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // First check if the user request already exists.  THIS is to prevent duplicate entries into the table.
            PreparedStatement friendsStatement = conn.prepareStatement("select * from invite where accepted = true and requestor = ?");
            friendsStatement.setString(1, userId);

            ResultSet rows = friendsStatement.executeQuery();
            while (rows.next()) {
                Friend friend = new Friend(rows.getString("requestee"), rows.getInt("requestid"));

                friendsList.add(friend);
                //LOGGER.log(Level.INFO, "[getFriendsWithExtraDataList] friend found: {0}", requestee);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
        return friendsList;
    }
    /**
     * Get 'from' and 'userKeycloakId' fields from http request.
     * This will help determine what data to return.
     * 
     * @param request
     * @return 
     */
    private void getRequestData(HttpServletRequest request) {
        String from = "";
        try (BufferedReader reader = request.getReader()) {

            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("from")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    sourceOfRequest = line.trim();
                }
                if (line.contains("userKeycloakId")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userKeycloakId = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }
    /**
     * Extracts requestId field from the http post request.
     * @param request
     * @return 
     */
    private String getUserKeycloakId(HttpServletRequest request) {
        String userKeycloakId = "";
        try (BufferedReader reader = request.getReader()) {

            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("userKeycloakId")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userKeycloakId = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return userKeycloakId;
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

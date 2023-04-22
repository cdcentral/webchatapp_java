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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
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
 * This will take requests from the front end to create a chat group.
 * 
 * So for example user A can create a chat group with users B, E and F, this 
 * servlet will receive that request and insert that to the database.
 */
public class CreateGroupChat extends HttpServlet {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CreateGroupChat.class.getName());

    /**
     * Need to parse out data as part of the group_chat data that was sent in the http post from the reactjs front end.
     */
    private static final String GROUP_CHAT_MEMBERS = "group_chat_members";
    private static final String GROUP_CHAT_OWNER = "group_chat_owner";

    /**
     * These two variables will contain the values passed in from the react gui.
     */
    private String groupChatMembers = "";
    private String groupChatOwner = "";

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

        extractUsersInGroup(request);
        
        JSONObject jsonResponse = new JSONObject();

        if (doesChatGroupAlreadyExist(groupChatMembers)) {
            // chat group already exists
            jsonResponse.put("status", "ignore");
            jsonResponse.put("reason", "chat group already exists.");
        }
        else if (groupChatMembers != null && !groupChatMembers.isEmpty() && groupChatOwner != null && !groupChatOwner.isEmpty()) {
            jsonResponse = createGroupChat();            
        } else {
            jsonResponse.put("status", "fail");
            jsonResponse.put("reason", "usersInGroup is empty.  Was expecting list of emails.");
        }

        try ( PrintWriter out = response.getWriter()) {
            // send data back to front end react app
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Checks to see if the chat group already exists in the database.
     * 
     * If it does it returns true, false otherwise.
     * @param usersInGroup
     * @return 
     */
    private boolean doesChatGroupAlreadyExist(String usersInGroup) {

        String[] usersGroupArray = usersInGroup.split(",");
        List<String> usersInGroupArray = Arrays.asList(usersGroupArray);
        // at this point I have the users chat group in an array list 


        boolean doesItAlreadyExist = false;
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            String query = "SELECT group_members from USER_CHAT_GROUPS WHERE group_members = ?";
            
            
            // First check if the user request already exists.  THIS is to prevent duplicate entries into the table.
            PreparedStatement queryStmt = conn.prepareStatement(query);
            queryStmt.setString(1, usersInGroup);

            ResultSet rs = queryStmt.executeQuery();
            while(rs.next() && doesItAlreadyExist == false) {
                doesItAlreadyExist = true;
            }
            // doing second query JUST TO BE SURE
            if (!doesItAlreadyExist) {
                query = "Select group_members from USER_CHAT_GROUPS"; // gets all
                Statement stmt = conn.createStatement();
                ResultSet rs2 = stmt.executeQuery(query);
                while (rs2.next() && !doesItAlreadyExist) {
                    String savedChatGroup = rs2.getString("group_members");
                    String[] savedUsersGroupArray = savedChatGroup.split(",");
                    List<String> savedUsersInGroupArray = Arrays.asList(savedUsersGroupArray);
                    
                    // compare if savedUsersInGroupArray and usersInGroupArray have same elements
                    // first.size() == second.size() && first.containsAll(second) && second.containsAll(first)
                    if (savedUsersInGroupArray.size() == usersInGroupArray.size()) {
                        LOGGER.log(Level.INFO, "savedUsersInGroupArray and usersInGroupArray match in size: {0}", savedUsersInGroupArray.size());
                        if (savedUsersInGroupArray.containsAll(usersInGroupArray)) {
                            if (usersInGroupArray.containsAll(savedUsersInGroupArray)) {
                                // here we make sure if matches
                                doesItAlreadyExist = true;
                            }
                        }
                    } else {
                        LOGGER.info("Saved Group Chat does not match size of new Group Chat (not saved yet)");
                    }
                }
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
        LOGGER.log(Level.INFO, "does {0} already exist in the USER_CHAT_GROUPS: {1}", new Object[]{usersInGroup, doesItAlreadyExist});
        return doesItAlreadyExist;
    }

    /**
     * Extracts the users chat group from the request object.
     * @param request
     * @return 
     */
    private void extractUsersInGroup(HttpServletRequest request) {

        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";

                if (line.contains(GROUP_CHAT_MEMBERS)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    groupChatMembers = line.trim();
                } else if (line.contains(GROUP_CHAT_OWNER)) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    groupChatOwner = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "[CreateGroupChat] msg groupChatMembers: {0}, chat owner: {1}", new Object[]{groupChatMembers, groupChatOwner});
            // here have the emails.  should I get user ids from keycloak? perhaps later.
            // For now just storing the emails should be ok.
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Inserts into the USER_CHAT_GROUPS the saved chat groups.
     * 
     * This includes a users group that has 'xyz' members in it.
     * 
     * Each user can have multiple chat groups.
     * @param usersInGroup String, contains the users emails that are part of this group.
     * @return 
     */
    private JSONObject createGroupChat() {
        JSONObject jsonResponse = new JSONObject();
        Connection conn = null;
       try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // insert the new chat group but also get the chat id that is automatically created.
            PreparedStatement insertStmt = 
                    conn.prepareStatement("INSERT into USER_CHAT_GROUPS (date_created, group_members, group_owner) VALUES(?, ?, ?) returning id", Statement.RETURN_GENERATED_KEYS);

            // 2021-03-24 16:48:05.591
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            insertStmt.setTimestamp(1, timestamp);
            insertStmt.setString(2, groupChatMembers);
            insertStmt.setString(3, groupChatOwner);

            int rows = insertStmt.executeUpdate();
            switch (rows) {
            // updated a row
                case 1:
                    jsonResponse.put("status", "success");
                    jsonResponse.put("reason", "insert rows equals 1");
                    break;
                default:
                    jsonResponse.put("status", "fail");
                    jsonResponse.put("reason", "odd.  Rows inserted should be 1 but actual rows updated: " + rows);
                    break;
            }

            int returnedID = -1;

            // This part returns the generated Chat ID.
            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    returnedID = generatedKeys.getInt(1);
                    LOGGER.log(Level.INFO, "[createGroupChat] Returned ID after insert: {0}", returnedID);
                }
                else {
                    LOGGER.severe("no ID obtained.");
                }
            }
            jsonResponse.put("chat_group_id", returnedID);
            // 
            LOGGER.log(Level.INFO, "[createGroupChat] update status: {0}", jsonResponse.toString());

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
        return jsonResponse;
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

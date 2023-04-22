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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet that removes a user from a chat group.
 * @author chris
 */
public class RemoveFromChatGroup extends HttpServlet {

    /**
     * The id of the specific chat group to remove the users email from.
     */
    private Integer chatIdToRemoveUserFrom = -1;
    
    /**
     * Email to remove from a chat group.
     */
    private String userEmailToRemove = "";

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RemoveFromChatGroup.class.getSimpleName());

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
        extractData(request);
        // if condition is true then we weren't able to extract all the user info
        if (chatIdToRemoveUserFrom == -1 || userEmailToRemove.isEmpty()) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "Either chat id or users email was not available");
        } else {
            jsonResponse = removeUserFromGroup();
        }

        try ( PrintWriter out = response.getWriter()) {

            out.println(jsonResponse.toString());
        }
    }

    /**
     * Handles deleting the chat group from the database.
     * @param chatGroupToDelete
     * @return 
     */
    private JSONObject removeUserFromGroup() {

        JSONObject jsonResponse = new JSONObject();
        Integer rowsEffected = -1;
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // do SQL select for group members
            String selectQuery = "select group_members from user_chat_groups where id = " + chatIdToRemoveUserFrom;
            Statement selectStmt = conn.createStatement();
            ResultSet dataReturned = selectStmt.executeQuery(selectQuery);
            String groupMembers = "";
            while(dataReturned.next()) {
                groupMembers = dataReturned.getString("group_members");
            }
            // now have group members
            if (groupMembers.isEmpty() == false) {
                // remove the user from the chat group members list.
                groupMembers = groupMembers.replace("," + userEmailToRemove, "");
                groupMembers = groupMembers.replace(userEmailToRemove + ",", "");
                groupMembers = groupMembers.replace(userEmailToRemove, "");
            }
            
            String updateSQL = "Update user_chat_groups set group_members = ? where id = ?";

            PreparedStatement updateChatGroupStmt = 
                    conn.prepareStatement(updateSQL);
            updateChatGroupStmt.setString(1, groupMembers);
            updateChatGroupStmt.setInt(2, chatIdToRemoveUserFrom);

            rowsEffected = updateChatGroupStmt.executeUpdate();
            LOGGER.log(Level.INFO, "[removeUserFromGroup] user to remove: {0}, group id to remove from: {1}, rowsEffected: {2}", 
                    new Object[]{userEmailToRemove, chatIdToRemoveUserFrom, rowsEffected});
            if (rowsEffected > 0) {
                jsonResponse.put("status", "success");
                jsonResponse.put("rowsEffected", rowsEffected);

                handleExitedMemberColumn(conn);

            } else {
                jsonResponse.put("status", "failure");
                jsonResponse.put("rowsEffected", 0);
            }
        } catch (SQLException | NamingException | JSONException ex) {
            LOGGER.log(Level.SEVERE, "[removeUserFromGroup] Exception: {0}", ex);
            jsonResponse.put("status", "failed");
            jsonResponse.put("rowsEffected", 0);
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

    /**
     * Updates the exited_members column with the user that just exited
     * @param conn Connection, database connection object
     * @return 
     */
    private boolean handleExitedMemberColumn(Connection conn) {
        boolean successful = false;
        PreparedStatement selectPS = null;
        PreparedStatement updatePS = null;
        try {
            // FIRST PART, getting exited_members value.
            // This will be used to know how to update the record with this new email.
            String isColumnNullSQL = "SELECT exited_members FROM user_chat_groups WHERE id = ?";
            selectPS = conn.prepareStatement(isColumnNullSQL);
            selectPS.setInt(1, chatIdToRemoveUserFrom);
            ResultSet columnNullSQLRS = selectPS.executeQuery();

            
            String exitedMembers = "";
            boolean empty = true;
            while(columnNullSQLRS.next()) {
                exitedMembers = columnNullSQLRS.getString("exited_members");
                break;
            }

            if (exitedMembers == null || exitedMembers.isEmpty()) {
                // just use 'userEmailToRemove'
                exitedMembers = userEmailToRemove;
            } else {
                exitedMembers += "," + userEmailToRemove;
            }
            String updateSQL = "UPDATE user_chat_groups SET exited_members = ?, exited_members_timestamp = ? where ID = ?";
            updatePS = conn.prepareStatement(updateSQL);
            updatePS.setString(1, exitedMembers);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            updatePS.setTimestamp(2, timestamp);
            updatePS.setInt(3, chatIdToRemoveUserFrom);
            int rowsEffected = updatePS.executeUpdate();
            if (rowsEffected > 0) {
                // successful in updating
                successful = true;
            } else {
                // not successful in updating
                successful = false;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Exception {0}", ex.getMessage());
            ex.printStackTrace();
        } finally {

            if (selectPS != null) {
                try {
                    selectPS.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Exception {0}", ex.getMessage());
                }
                selectPS = null;
            }

            if (updatePS != null) {
                try {
                    updatePS.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Exception {0}", ex.getMessage());
                }
                updatePS = null;
            }
        }
        return successful;
    }
    /**
     * Extracts user chat group from http request.
     * @param request
     * @return 
     */
    private void extractData(HttpServletRequest request) {

        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("userChatGroupToRemoveUser")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    chatIdToRemoveUserFrom = Integer.parseInt(line.trim());
                } else if (line.contains("userEmailToRemove")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userEmailToRemove = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
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

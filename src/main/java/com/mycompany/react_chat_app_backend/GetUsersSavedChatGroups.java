/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import com.mycompany.react_chat_app_backend.util.ChatGroups;
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
 * This retrieves the users current saved set of chat groups.
 * 
 * This will be displayed on the left hand panel of the react front end.
 * 
 * The user can select any of these chat groups and begin to send messages.
 *
 */
public class GetUsersSavedChatGroups extends HttpServlet {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GetUsersSavedChatGroups.class.getName());

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
        
        //String userKeycloakId = getUserKeycloakId(request);
        JSONObject jsonResponse = new JSONObject();
        ArrayList<ChatGroups> savedChatGroups = null;
        String userKeycloakEmail = getUserKeycloakEmail(request);
        if ( userKeycloakEmail != null && !userKeycloakEmail.isEmpty()) {

            savedChatGroups = getSavedGroups(userKeycloakEmail);
        }

        jsonResponse.put("savedChatGroups", savedChatGroups);

        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }
    /**
     * Get list of Saved Groups that the user has a chat for.
     * @param userKeycloakId
     * @return 
     */
    private ArrayList<ChatGroups> getSavedGroups(String userKeycloakEmail) {
        ArrayList<ChatGroups> savedChatGroups = new ArrayList<ChatGroups>();
        
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            String query = "select id, group_members from user_chat_groups where group_members like '%" + userKeycloakEmail + "%'";
            PreparedStatement savedGroupsStatement = conn.prepareStatement(query);

            ResultSet rows = savedGroupsStatement.executeQuery();
            while (rows.next()) {
                String groupMembers = rows.getString("group_members");
                Integer groupID = rows.getInt("id");
                ChatGroups cGroups = new ChatGroups(groupID, groupMembers);

                savedChatGroups.add(cGroups);

                LOGGER.log(Level.INFO, "[GetUserChatGroups] group: {0}", groupMembers);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[GetUserChatGroups] Exception: {0}", ex);
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
        return savedChatGroups;
    }

    /**
     * Extracts the users email that is stored in keycloak.
     * 
     * This will be used to extract the users saved chat groups.
     * 
     * @param request
     * @return 
     */
    private String getUserKeycloakEmail(HttpServletRequest request) {
        String userKeycloakEmail = "";
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("userKeycloakEmail")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userKeycloakEmail = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return userKeycloakEmail;
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
            Logger.getLogger(GetUsersSavedChatGroups.class.getName()).log(Level.INFO, "msg: " + msg);
        } catch (IOException ex) {
            Logger.getLogger(GetUsersSavedChatGroups.class.getName()).log(Level.SEVERE, null, ex);
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

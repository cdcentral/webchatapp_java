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
 * This will take a request from the front end to delete an existing user chat group.
 * 
 * @author chris
 */
public class DeleteChatGroup extends HttpServlet {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DeleteChatGroup.class.getName());

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
        String chatGroupToDelete = extractUserChatGroup(request);
        Integer chatGroupIDToDelete = Integer.parseInt(chatGroupToDelete);
        jsonResponse = deleteChatGroup(chatGroupIDToDelete);

        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Handles deleting the chat group from the database.
     * @param chatGroupToDelete
     * @return 
     */
    private JSONObject deleteChatGroup(Integer chatGroupToDelete) {

        JSONObject jsonResponse = new JSONObject();
        Integer rowsEffected = -1;
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            String deleteSQL = "delete from user_chat_groups where id = " + chatGroupToDelete + "";
            PreparedStatement deleteChatGroupStmt = 
                    conn.prepareStatement(deleteSQL);

            rowsEffected = deleteChatGroupStmt.executeUpdate();
            LOGGER.log(Level.INFO, "[deleteChatGroup] chatGroupToDelete: {0}, rowsEffected: {1}", new Object[]{chatGroupToDelete, rowsEffected});
            if (rowsEffected > 0) {
                jsonResponse.put("status", "success");
                jsonResponse.put("rowsEffected", rowsEffected);
                // something was deleted
            } else {
                jsonResponse.put("status", "failure");
                jsonResponse.put("rowsEffected", 0);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[DeleteChatGroup] Exception: {0}", ex);
            jsonResponse.put("status", "failure");
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
     * Extracts user chat group from http request.
     * @param request
     * @return 
     */
    private String extractUserChatGroup(HttpServletRequest request) {
        String userChatGroupToDelete  = "";
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("userChatGroupToDelete")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    userChatGroupToDelete = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return userChatGroupToDelete;
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

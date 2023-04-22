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
 * This servlet handles when a user clicks on the email link to accept a friend request
 * to join the chat.

 */
public class InviteAccepted extends HttpServlet {
    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(InviteAccepted.class.getName());

    // test email link to use on front end (reactjs which will send a request here).
    // http://localhost:3000/InviteAccepted/?rid=1

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

        String requestID = getRequestId(request);

        // now update the db saying the invite was successful.
        JSONObject retStatus = updateFriendInvite(requestID);

        try ( PrintWriter out = response.getWriter()) {
            out.println(retStatus.toString());
        }
    }

    /**
     * Creates a database connection then updates the friend invite request to 
     * indicate that it was accepted.
     * @param requestId
     * @return 
     */
    private JSONObject updateFriendInvite(String requestId) {

        Integer requestIdInt = Integer.valueOf(requestId);
        JSONObject jsonResponse = new JSONObject();
        Connection conn = null;

        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // updates the invite record to indicate that the user accepted the invite.
            PreparedStatement updateStmt = conn.prepareStatement("update invite set accepted = true where requestid = ?");
            updateStmt.setInt(1, requestIdInt);

            int rows = updateStmt.executeUpdate();
            switch (rows) {
            // updated a row
                case 1:
                    jsonResponse.put("status", "success");
                    jsonResponse.put("reason", "updated rows equals 1");
                    break;
            // no rows were updated.  weird..
                case 0:
                    jsonResponse.put("status", "fail");
                    jsonResponse.put("reason", "rows updated is 0. Weird. Why is there no match? This means that the insert in InviteRequest.java failed despite the email being sent.");
                    break;
            // really weird case
                default:
                    jsonResponse.put("status", "fail");
                    jsonResponse.put("reason", "odd.  Rows updated should be 1 but actual rows updated: " + rows);
                    break;
            }

            // 
            LOGGER.log(Level.INFO, "[InviteAccepted] update status: {0}", jsonResponse.toString());

        } catch (Exception ex) {
            LOGGER.severe(ex.toString());

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
     * Extracts requestId field from the http post request.
     * @param request
     * @return 
     */
    private String getRequestId(HttpServletRequest request) {
        String requestId = "";
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("requestId")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    requestId = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return requestId;
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

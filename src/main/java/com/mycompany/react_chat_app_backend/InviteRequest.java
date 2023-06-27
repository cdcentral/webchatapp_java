/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import com.mycompany.react_chat_app_backend.util.Email;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * This servlet handles sending an email invite link to another user.
 * 
 * This link is an invitation for the user to join this web app.
 * 
 * @author chris
 */
public class InviteRequest extends HttpServlet {

    /**
     * Class Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(InviteRequest.class.getName());

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

        LOGGER.info("[InviteRequest] Received request.");
        String emailToInvite = "";
        String requestor = "";

        Integer requestId = -1; // this will be used for the invite request link

        /*
            Read data from the requests buffered reader
        */
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("requestee")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    emailToInvite = line.trim();
                } else if (line.contains("requestor")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    requestor = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        JSONObject jsonResponse = new JSONObject();
        if (requestor.isEmpty() || emailToInvite.isEmpty()) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "requestor or email is empty");
            try ( PrintWriter out = response.getWriter()) {
                out.println(jsonResponse);
            }
            return;
        }

        Connection conn = null;
       
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();


            // Does select query to see if this user already received a request but did not accept yet.
            PreparedStatement queryStmt = conn.prepareStatement("select * from invite where requestor = ? and requestee = ? and accepted = false");
            queryStmt.setString(1, requestor);
            queryStmt.setString(2, emailToInvite);
            
            ResultSet result = queryStmt.executeQuery();
            boolean friendRequestExists = false;
            while(result.next()) {
                friendRequestExists = true;
            }

            // only inserts request to the table if the friend request doesn't exist.
            if (!friendRequestExists) {
                PreparedStatement insertStmt = conn.prepareStatement("insert into INVITE (requestor, requestee) Values ( ?, ?)");
                insertStmt.setString(1, requestor);
                insertStmt.setString(2, emailToInvite);

                int rows = insertStmt.executeUpdate();
                if (rows > 0) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("reason", "rows updated " + rows);
                } else {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "rows updated is 0 or less");                
                }
            } else {
                jsonResponse.put("status", "success");
                jsonResponse.put("reason", "friend request already exists. Not inserting again.");
                LOGGER.info("[InviteRequest] friend request invite already exits in DB.  Not inserting again.");
            }
            // get requestID.  This will be used as part of the invite request link
            if (jsonResponse.getString("status").equals("success")) {

                PreparedStatement requestIdQueryStmt = conn.prepareStatement("select requestid from invite where requestor = ? and requestee = ? and accepted = false");
                requestIdQueryStmt.setString(1, requestor);
                requestIdQueryStmt.setString(2, emailToInvite);

                ResultSet resultSet = requestIdQueryStmt.executeQuery();

                while(resultSet.next()) {
                    requestId = resultSet.getInt("requestid");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "exception: {0}", ex);
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

        String smtpEndpoint = getServletConfig().getInitParameter("smtpEndpoint");
        String emailFrom = getServletConfig().getInitParameter("fromEmail");
        String awsRegion = getServletConfig().getInitParameter("awsRegion");
        String awsSecret = getServletConfig().getInitParameter("awsSecretName");
        // SEND EMAIL.
        Email email = new Email(emailToInvite, emailFrom, smtpEndpoint, awsRegion, awsSecret);
        boolean retStatus = email.sendEmail(requestId);
        jsonResponse.put("emailSentStatus", retStatus);

        // handle its response.
        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Gets data that was in the POST message from react js.
     * @param request HttpServletRequest
     * @return 
     */
    private String getData(HttpServletRequest request) {
        String emailToInvite = "";
        /*
            Read data from the requests buffered reader
        */
        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("emailToInvite")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    emailToInvite = line.trim();
                } 
            }
            LOGGER.info("msg: " + msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return "";
    }

    /**
     * 
     */
    private void updateDatabase() {
        Connection conn = null;
       
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();
            
            Statement stmt = conn.createStatement();
            
            String queryOrderByLimit = "select * from invite";// ORDER BY msg_id DESC LIMIT 4";
            
            ResultSet rs = stmt.executeQuery(queryOrderByLimit);
            while (rs.next()) {
                String requestor = rs.getString("requestor");
                String requestee = rs.getString("requestee");
            }
            
        } catch (Exception ex) {
            LOGGER.severe("Exception in: " + ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.severe("Exception in: " + ex);
                }
                conn = null;
            }
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

        response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000"); // asterisk BAD, change to be specific domain.
        //response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000/*"); // asterisk BAD, change to be specific domain.
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
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000"); // asterisk BAD, change to be specific domain.
        //response.addHeader("Access-Control-Allow-Origin", "http://localhost:3000/*"); // asterisk BAD, change to be specific domain.
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

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import java.io.PrintWriter;

import java.io.IOException;
import java.io.InputStream;
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
import javax.servlet.http.Part;
import javax.sql.DataSource;
import org.json.JSONObject;
/**
 *
 * @author chris
 */
public class SaveImage extends HttpServlet {


    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SaveImage.class.getName());
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
        InputStream inputStream = null;
        try {
            // for this to work, needed to add <multipart-config> tags into web.xml
            Part filePart = request.getPart("image");
            if (filePart != null) {
                inputStream = filePart.getInputStream();
                saveImageToDatabase(inputStream, jsonResponse);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception -> ", e);
            //e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving image");
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        try ( PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println(jsonResponse.toString());
        }
    }

    /**
     * 
     * @param inputStream
     * @throws SQLException 
     */
    private void saveImageToDatabase(InputStream inputStream, JSONObject jsonResponse) throws SQLException {

        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();
            String sql = "INSERT INTO images (image) Values (?)";
            //String sql = "INSERT INTO images (image, userKeycloakId) Values (?, ?)";//userKeycloakId
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setBinaryStream(1, inputStream);
            //statement.setString(2, "abcdefg123456"); // dummy data for now.
            int rows = statement.executeUpdate();
            LOGGER.log(Level.INFO, "rows effected -> " + rows);
            // auto commit is enabled, so don't need conn.commit.
            jsonResponse.put("RowsEffected", rows);
            jsonResponse.put("Success", ((rows > 0)));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception saving image into database -> ", ex);
            jsonResponse.put("RowsEffected", -1);
            jsonResponse.put("Success", false);
            
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
//        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
//            String sql = "INSERT INTO images (image_data) VALUES (?)";
//            PreparedStatement statement = conn.prepareStatement(sql);
//            statement.setBinaryStream(1, inputStream);
//            statement.executeUpdate();
//        }
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

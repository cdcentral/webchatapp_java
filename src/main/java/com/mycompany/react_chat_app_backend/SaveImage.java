/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
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
     * The user making the avatar selection for their friend.
     */
    private String keycloakID = "";

    /**
     * Friend who is about the have their image saved by the user keycloak id.
     */
    private String friend = "";

    /**
     * String representation of the image to be saved.
     */
    private String img = "";

    /**
     * Byte array representation of the image to be saved.
     */
    private byte[] imgByteArray;

    /**
     * Parse out fields in post request.
     * @param request 
     */
    private void parseRequest(HttpServletRequest request) {

        try (BufferedReader reader = request.getReader()) {
            
            String line = "";
            String msg = "";
            while ((line = reader.readLine()) != null) {
                msg += line + "\n";
                if (line.contains("image")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    img = line.trim();
                } else if (line.contains("friend")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    friend = line.trim();
                } else if (line.contains("userKeycloakId")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakID = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "msg: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        if (!img.isEmpty()) {
            displayImageInJFrame(false);
        }
    }
    /**
     * Helper test method to verify the image can be displayed in a jframe.
     */
    private void displayImageInJFrame(boolean show) {
        if (!show) {
            return;
        }
        String base64Image = img.split(",")[1];
        imgByteArray = DatatypeConverter.parseBase64Binary(base64Image);
        byte[]imageBytes = DatatypeConverter.parseBase64Binary(base64Image);

        /*
        Works below.  I can display the image received from React App.
        */
        try {
               final BufferedImage bufImg = ImageIO.read(new ByteArrayInputStream(imageBytes));

            JFrame frame = new JFrame("Image");
              //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
              //frame.setSize(img.getWidth(), img.getHeight());
              frame.setSize(200, 200);
              frame.add(new JPanel() {
                public void paint(Graphics g) {
                  g.drawImage(bufImg, 0, 0, null);
                }
              });
              frame.show();
        } catch (IOException ex) {
            Logger.getLogger(SaveImage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
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

        parseRequest(request);
        JSONObject jsonResponse = new JSONObject();

        try {
            if (!keycloakID.isEmpty() && !friend.isEmpty() && !img.isEmpty()) {
                boolean exists = doesDataExist();
                if (!exists) {
                    // check if image/data already exists
                    saveImageToDatabase(jsonResponse);
                } else {
                    // update
                    updateImageToDatabase(jsonResponse);
                }
            } else {
                    jsonResponse.put("RowsEffected", -1000);
                    jsonResponse.put("Success", "fail");
                    jsonResponse.put("Reason", "Could not extract image/keycloak id/friend data");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception -> ", ex);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving image");
        }

        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Checks if saved image data already exists for the same friend and user keycloak id.
     * This will help prevent duplicate data being inserted into the table
     * @return 
     */
    private boolean doesDataExist() {
        boolean exists = false;
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();
            String sql = "SELECT * from images where user_keycloak_id = ? and friend=?";// and image = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keycloakID);
            statement.setString(2, friend);
            //statement.setBinaryStream(3, imageInputStream);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                exists = true;
                break;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception trying to see if data exists -> ", ex);
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
        return exists;
    }

    /**
     * Updates friends avatar with new image.
     * 
     * @param jsonResponse JSONObject
     * @throws SQLException 
     */
    private void updateImageToDatabase(JSONObject jsonResponse) throws SQLException {

        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();
            String sql = "UPDATE images SET image = ?, image_str = ? where user_keycloak_id=? and friend=?";
            
            PreparedStatement statement = conn.prepareStatement(sql);
            //statement.setBinaryStream(1, inputStream);
            //statement.setBytes(1, byteArr);
            statement.setBytes(1, imgByteArray);
            statement.setString(2, img);
            statement.setString(3, keycloakID);
            statement.setString(4, friend);

            int rows = statement.executeUpdate();
            LOGGER.log(Level.INFO, "rows effected -> {0}", rows);
            // auto commit is enabled, so don't need conn.commit.
            jsonResponse.put("RowsEffected", rows);
            jsonResponse.put("Success", ((rows > 0)));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception updating image into database -> ", ex);
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
    }


    /**
     * Saves the friends avatar to the database for the first time.
     * 
     * @throws SQLException 
     */
    private void saveImageToDatabase(JSONObject jsonResponse) throws SQLException {

        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();
            String sql = "INSERT INTO images (user_keycloak_id, friend, image, image_str) Values (?,?,?,?)";

            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keycloakID);
            statement.setString(2, friend);
            //statement.setBinaryStream(3, inputStream);
            statement.setBytes(3, imgByteArray);
            statement.setString(4, img);

            int rows = statement.executeUpdate();
            LOGGER.log(Level.INFO, "rows effected -> {0}", rows);
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

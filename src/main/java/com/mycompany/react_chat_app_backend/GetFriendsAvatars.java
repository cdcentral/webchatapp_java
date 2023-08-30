/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.mycompany.react_chat_app_backend;

import com.mycompany.react_chat_app_backend.util.FriendAvatars;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author chris
 */
public class GetFriendsAvatars extends HttpServlet {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GetFriendsAvatars.class.getSimpleName());

    /**
     * Keycloak ID of the user logged in.  Will use this partly to get list of his
     * friends and their avatars.
     */
    private String keycloakID= "";

    /**
     * Keycloak email of the user logged in.  Will use this partly to get list of his 
     * friends and their avatars.
     */
    private String keycloakEmail = "";

    /**
     * Access token from Keycloak to do http rest api requests.
     */
    private String accessTokenResponse = "";

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

        // extract http request data from react front end
        extractDataFromRequest(request);

        // do sql commands to get list of friends and images/avatars
        ArrayList<FriendAvatars> friends = getFriendsAndAvatarsDBData();

        // send data back
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("friends", friends);
        try ( PrintWriter out = response.getWriter()) {
            out.println(jsonResponse.toString());
        }
    }

    /**
     * Makes SQL statements to get the users friends and their associated avatars.
     * 
     * @return 
     */
    private ArrayList<FriendAvatars> getFriendsAndAvatarsDBData() {

        String otherUsersEmail = "";
        ArrayList<FriendAvatars> friendsV2 = new ArrayList<>();
        Connection conn = null;
        try {
            InitialContext ctxt = new InitialContext();
            
            DataSource ds = (DataSource)ctxt.lookup("java:/comp/env/jdbc/postgres");
            conn = ds.getConnection();

            // Logic error.  This works fine if you log in as the person who made the friend requests that accepted.
            // BUT if you log in as the friend who got the request to join and accept then this won't work.
            String query = "select requestee,requestid from invite where accepted = true and requestor = ?";
            
            // First check if the user request already exists.  THIS is to prevent duplicate entries into the table.
            PreparedStatement friendsStatement = conn.prepareStatement(query);//query);
            friendsStatement.setString(1, keycloakID);
            //friendsStatement.setString(2, keycloakEmail);

            ResultSet rows = friendsStatement.executeQuery();
            while (rows.next()) {
                FriendAvatars friendAvatars = new FriendAvatars();
                friendAvatars.setFriend(rows.getString("requestee"));           // this is the users email.
                friendAvatars.setFriendRequestId(rows.getInt("requestid"));

                friendsV2.add(friendAvatars);
            }

            if (!friendsV2.isEmpty()) {
                // COPY CODE FROM BELOW THAT DOES SELECT * FROM IMAGES....
                String sql = "SELECT * from images where user_keycloak_id = ?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, keycloakID);

                ResultSet results = statement.executeQuery();
                while (results.next()) {

                        String _friend = results.getString("friend");
                        for (int i = 0; i < friendsV2.size(); i++) {
                            String friend2 = friendsV2.get(i).getFriend();
                            if (_friend.equals(friend2)) {
                                friendsV2.get(i).setAvatarImage(results.getBytes("image"));
                                friendsV2.get(i).setAvatarImageString(results.getString("image_str"));
                                displayImageTestV3((byte[])friendsV2.get(i).getAvatarImage(), false);
                                break;
                            }
                        }
                }
            }

            if (friendsV2.isEmpty()) {

                // 
                String queryV2 = "select requestee,requestid,requestor from invite where accepted = true and requestee = ?";
                friendsStatement = conn.prepareStatement(queryV2);//query);
                friendsStatement.setString(1, keycloakEmail);
                ResultSet rowsV2 = friendsStatement.executeQuery();
                while (rowsV2.next()) {
                    String requesteeEmail = rowsV2.getString("requestee"); // email of the person that was requested to join
                    Integer requestID = rowsV2.getInt("requestid");     // unique id of request
                    String otherUsersKeycloakID = rowsV2.getString("requestor");    // keycloak user id of the user making the request. Use this to get the users email.
                                                    // SHOULD PROBABLY MAKE ANOTHER TABLE where the keycloak ID is associated to the users email
                                                    // WHEN USER LOGS IN DO: 
                                                    // 1. check if keycloak id and email exist in chat_users table
                                                    // 2. if it doesn't add it in.
                                                    // 3. if it does then do nothing
                                                    // THEN
                                                    // THIS code here can use the keycloak id from the requestor object and get the users email.
                    otherUsersEmail = getUserEmailFromKeycloak(otherUsersKeycloakID);

                    FriendAvatars friendAvatars = new FriendAvatars();
                    friendAvatars.setFriend(otherUsersEmail);
                    friendAvatars.setFriendRequestId(requestID);

                    friendsV2.add(friendAvatars);
                    keycloakID = otherUsersKeycloakID;
                }
                if (!friendsV2.isEmpty()) {
                    for(int i = 0; i < friendsV2.size(); i++) {
                        String friend = friendsV2.get(i).getFriend();
                        // do another select statement from images.  Perhaps using the 'otherUsersEmail' from above.
                        String sql = "SELECT * from images where friend = ?";
                        PreparedStatement statement = conn.prepareStatement(sql);
                        statement.setString(1, friend);
                        
                        ResultSet rs = statement.executeQuery();
                        while (rs.next()) {
                            friendsV2.get(i).setAvatarImage(rs.getBytes("image"));
                            friendsV2.get(i).setAvatarImageString(rs.getString("image_str"));
                        }
                    }

                }
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
        return friendsV2;
    }

    /**
     * First getting access/admin token. 
     * 
     * Then using this token will make appropriate http call to get the users email from their keycloak id.
     */
    private String getUserEmailFromKeycloak(String otherUsersKeycloakID) {
        String emailToReturn = "";
        accessTokenResponse = getAccessToken(); // have to parse this out as this value is in the format -> {"access_token":"eyJhbGciOiJS....", "token_type":"Bearer".....}
        JSONObject jsonObj = new JSONObject(accessTokenResponse);
        accessTokenResponse = jsonObj.getString("access_token");
        if (!accessTokenResponse.isEmpty()) {
            emailToReturn = getKeycloakUsers(otherUsersKeycloakID);
        }
        return emailToReturn;
    }


    /**
     * Get users for a realm. 
     * @return 
     */
    private String getKeycloakUsers(String otherUsersKeycloakID) {//http://localhost:8080/auth/admin/realms/RealmOne/users
        String emailToReturn = "";
        String keycloakUrl = "http://localhost:8081/auth/admin/realms/ReactChatApp/users";
        BufferedReader in = null;
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(keycloakUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");       
            con.setRequestProperty("Authorization", "bearer " + accessTokenResponse);

            int status = con.getResponseCode();
            in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JSONArray jarray = new JSONArray(content.toString());
            for(int i = 0; i < jarray.length(); i++) {
                JSONObject jobj = jarray.getJSONObject(i);
                String keycloakID = String.valueOf(jobj.get("id"));
                if (keycloakID.equals(otherUsersKeycloakID)) {
                    emailToReturn = String.valueOf(jobj.get("email")); // email
                    LOGGER.info("FOUND MATCHING KEYCLOAK ID");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(GetFriendsAvatars.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(GetFriendsAvatars.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return emailToReturn;
    }
    /**
     * Makes http admin rest api request to keycloak to get an access token.
     * 
     * This access token is used to make subsequent requests to keycloak.
     * 
     * @return String value containing the access token.
     */
    private String getAccessToken() {
        StringBuffer content = new StringBuffer();
        String keycloakUrl = "http://localhost:8081/auth/realms/master/protocol/openid-connect/token";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", "admin-cli");
        parameters.put("username", "admin");
        parameters.put("password", "BAD_PASSWORD");
        parameters.put("grant_type", "password");

        BufferedReader in = null;
        try {
            URL url = new URL(keycloakUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");            
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(getParamsString(parameters));
            out.flush();
            out.close();
            int status = con.getResponseCode();
            in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            System.out.println("Get Keycloak token -> " + content.toString());
        } catch (Exception ex) {
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(GetFriendsAvatars.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return content.toString();
    }

    /**
     * Helper method that places data that is supposed to be sent in the http post.
     * 
     * In this specific case it will set the post data with the keycloak endpoint 
     * to get the access credentials.
     * 
     * @param params Map<String, String>
     * @return
     * @throws UnsupportedEncodingException 
     */
    private  String getParamsString(Map<String, String> params) 
      throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
          result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
          result.append("=");
          result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
          result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
          ? resultString.substring(0, resultString.length() - 1)
          : resultString;
    }

    /**
     * Test method.  Takes byte array of image stored in the postgres database
     * and displays it in a JFrame window.
     * 
     * Dev Note: Set the second parameter to true in the calling function to see the image.
     * @param imgArr 
     */
    private void displayImageTestV3(byte[] imgArr, boolean showImage) {

        if (!showImage) {
            return;
        }
        try {
            final BufferedImage bufImg = ImageIO.read(new ByteArrayInputStream(imgArr));
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[displayImageTestV3] Exception -> {0}", ex);
        }
        
    }

    /**
     * Extracts users keycloak id and email from the request.
     * 
     * @param request
     * @return 
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
                if (line.contains("userKeycloakId")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakID = line.trim();
                } else if (line.contains("userKeycloakEmail")) {
                    // read ahead
                    line = reader.readLine();
                    msg += line + "\n";
                    line = reader.readLine();
                    msg += line + "\n";
                    keycloakEmail = line.trim();
                }
            }
            LOGGER.log(Level.INFO, "Message received: {0}", msg);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        LOGGER.log(Level.INFO, "Keycloak ID: {0}, Keycloak Email: {1}", new Object[]{keycloakID, keycloakEmail});
        dataExtracted = (!keycloakID.isEmpty() && !keycloakEmail.isEmpty());
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

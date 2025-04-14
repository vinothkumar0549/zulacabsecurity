package com.example.controller;
import org.json.*;
import com.example.database.DatabaseStorage;
import com.example.database.Storage;
import com.example.pojo.User;
import com.example.service.CabService;
import com.example.util.Gender;
import com.example.util.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cab")
public class ZulacabController {

    Storage storage = new DatabaseStorage();

    CabService cabservice = new CabService(storage);

    @GET
    @Path("/hello")
    @Produces(MediaType.APPLICATION_JSON)
    public Response hello(){
        return Response.status(Response.Status.OK).entity("Hello World").build();
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(String RequestBody, @Context HttpServletRequest request) {

        try {
            // Parse the incoming JSON request
            JSONObject json = new JSONObject(RequestBody);

            // Extract user-related data
            JSONObject userJson = json.getJSONObject("user");

            String name = userJson.getString("name");
            String username = userJson.getString("username");
            String password = userJson.getString("password");
            int age = userJson.getInt("age");
            Gender gender = Gender.valueOf(userJson.getString("gender"));
            Role role = Role.valueOf(userJson.getString("role"));
            String adminusername = null;
            String adminpassword = null;
            String cablocation = null;
            if( role == Role.CAB) {
                // Extract userid and password
            adminusername = json.getString("username");
            adminpassword = json.getString("password");
            cablocation = json.getString(cablocation);
            }

            // You can now create a User object using the extracted data
            User user = new User();
            user.setUsername(username);
            user.setEncryptedpassword(password);
            user.setName(name);
            user.setAge(age);
            user.setGender(gender);
            user.setRole(role);

            // Log or use these values in the registration logic
            System.out.println("Username: " + username);
            System.out.println("Userid: " + adminusername);
            System.out.println("Password: " + adminpassword);
            
            // Call the service to register the user
            int id = cabservice.register(user, adminusername, adminpassword, cablocation);

            return Response.status(Response.Status.OK).entity("{\"error\": \"" + id + "\"}").build();

        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String RequestBody, @Context HttpServletRequest request) {

        JSONObject json = new JSONObject(RequestBody);
        String username = json.getString("username");
        String password = json.getString("password");

        if(username == null || password == null){
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Invalid Arguments\"}").build();
        }

        try {
            User user = cabservice.login(username, password);
            return Response.status(Response.Status.OK).entity(user).build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        
    }

    @POST
    @Path("/bookcab")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bookcab(String RequestBody, @Context HttpServletRequest request) {
        JSONObject json = new JSONObject(RequestBody);
        String username = json.getString("username");
        String password = json.getString("password");
        String source = json.getString("source");
        String destination = json.getString("destination");

        try {
            cabservice.bookcab(username, password, source, destination);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}


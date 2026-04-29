package com.carrental;

import com.carrental.model.User;
import com.carrental.service.AuthService;
import com.carrental.ui.AdminMenu;
import com.carrental.ui.CustomerMenu;
import com.carrental.util.ConsoleIO;

import java.sql.SQLException;

/**
 * Entry point for the Car Rental Management System.
 *
 * <pre>
 *   javac (see build.sh) -> java -cp ... com.carrental.Main
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        AuthService auth = new AuthService();

        System.out.println("==============================================");
        System.out.println("   Car Rental Management System  (Java + JDBC)");
        System.out.println("==============================================");

        while (true) {
            ConsoleIO.title("LOGIN");
            String user = ConsoleIO.readLine("Username (or 'exit'): ");
            if (user.equalsIgnoreCase("exit")) break;
            String pwd  = ConsoleIO.readLine("Password: ");

            User u;
            try {
                u = auth.login(user, pwd);
            } catch (SQLException ex) {
                System.out.println("DB error: " + ex.getMessage());
                continue;
            }
            if (u == null) {
                System.out.println("Invalid credentials.");
                continue;
            }
            System.out.println("Welcome, " + u.getFullName() + " (" + u.getRole() + ")");

            switch (u.getRole()) {
                case ADMIN    -> new AdminMenu().show();
                case CUSTOMER -> new CustomerMenu(auth).show();
            }
            auth.logout();
        }

        System.out.println("Goodbye.");
    }
}

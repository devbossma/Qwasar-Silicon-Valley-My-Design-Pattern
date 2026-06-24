package dev.saberlabs.db;

import java.sql.Connection;

public class DBDemo {
    public static void main(String[] args) {
       try {
           DatabaseUtil.initialize();
       } catch (Exception e) {
           System.out.println("[DBDemo] FAIL - Error occurred while initializing database.");
       }
    }
}

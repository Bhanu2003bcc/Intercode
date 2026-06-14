package com.interview.platform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String[] passwords = {"Admin@123", "Interviewer@123", "Candidate@123"};
        for (String password : passwords) {
            String hash = encoder.encode(password);
            boolean matches = encoder.matches(password, hash);
            System.out.println("PASSWORD: " + password);
            System.out.println("HASH: " + hash);
            System.out.println("VERIFIED: " + matches);
            System.out.println("----------------------------------------");
        }
    }
}

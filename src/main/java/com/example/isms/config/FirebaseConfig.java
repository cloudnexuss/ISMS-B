package com.example.isms.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.FirebaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing Firebase from environment variable...");

                // Load credentials from FIREBASE_SERVICE_ACCOUNT environment variable
                String firebaseCredentials = System.getenv("FIREBASE_SERVICE_ACCOUNT");
                if (firebaseCredentials == null || firebaseCredentials.isEmpty()) {
                    throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT environment variable is not set");
                }
                InputStream serviceAccount = new ByteArrayInputStream(
                        firebaseCredentials.getBytes(StandardCharsets.UTF_8));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://isms-f64f7.firebaseio.com/")
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully using environment variable");
                return app;
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase from environment variable: ", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    @Bean
    public Firestore getFirestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
package config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class FirestoreFactory {
    private static Firestore firestore;

    private FirestoreFactory() { }

    public static synchronized Firestore obter() {
        if (firestore != null) return firestore;
        try {
            if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(opcoes());
            firestore = FirestoreClient.getFirestore();
            return firestore;
        } catch (Exception e) {
            throw new IllegalStateException("Configure as credenciais do Firebase para conectar ao Firestore.", e);
        }
    }

    private static FirebaseOptions opcoes() throws Exception {
        String chaveJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        String emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        boolean emulator = emulatorHost != null && !emulatorHost.isBlank();
        GoogleCredentials credenciais;
        if (emulator) {
            credenciais = GoogleCredentials.create(new com.google.auth.oauth2.AccessToken(
                    "emulator-local-token", Date.from(Instant.now().plusSeconds(3600))));
        } else if (chaveJson == null || chaveJson.isBlank()) {
            credenciais = GoogleCredentials.getApplicationDefault();
        } else {
            credenciais = GoogleCredentials.fromStream(new ByteArrayInputStream(chaveJson.getBytes(StandardCharsets.UTF_8)));
        }
        FirebaseOptions.Builder opcoes = FirebaseOptions.builder().setCredentials(credenciais);
        String projeto = System.getenv("GOOGLE_CLOUD_PROJECT");
        if ((projeto == null || projeto.isBlank()) && emulator) projeto = System.getenv("GCLOUD_PROJECT");
        if (projeto != null && !projeto.isBlank()) opcoes.setProjectId(projeto);
        return opcoes.build();
    }
}

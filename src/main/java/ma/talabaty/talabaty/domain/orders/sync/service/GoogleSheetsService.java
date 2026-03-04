package ma.talabaty.talabaty.domain.orders.sync.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final String APPLICATION_NAME = "Talabaty Excel Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private final ObjectMapper objectMapper;

    public GoogleSheetsService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create Google Sheets service instance using stored credentials
     */
    public Sheets getSheetsService(String credentialsJson, String accessToken, String refreshToken) 
            throws GeneralSecurityException, IOException {
        
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        if (credentialsJson == null || credentialsJson.isEmpty()) {
            throw new IllegalArgumentException("Credentials are required");
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new StringReader(credentialsJson));
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        // Try to load existing credential from data store
        Credential credential = flow.loadCredential("user");
        
        // If no stored credential, use OAuth flow to get new tokens
        if (credential == null) {
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
        
        // If we have stored tokens, update the credential
        if (accessToken != null && !accessToken.isEmpty()) {
            credential.setAccessToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            credential.setRefreshToken(refreshToken);
        }
        
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Read data from Google Sheet
     */
    public List<List<Object>> readSheetData(String spreadsheetId, String sheetName, 
                                           String credentialsJson, String accessToken, String refreshToken) 
            throws IOException, GeneralSecurityException {
        
        Sheets service = getSheetsService(credentialsJson, accessToken, refreshToken);
        String range = sheetName + "!A:Z"; // Read columns A to Z
        
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        
        List<List<Object>> values = response.getValues();
        return values != null ? values : Collections.emptyList();
    }

    /**
     * Get the number of rows in the sheet
     */
    public int getRowCount(String spreadsheetId, String sheetName,
                          String credentialsJson, String accessToken, String refreshToken)
            throws IOException, GeneralSecurityException {
        
        List<List<Object>> data = readSheetData(spreadsheetId, sheetName, credentialsJson, accessToken, refreshToken);
        return data.size();
    }

    /**
     * Extract client ID from credentials JSON
     */
    private String getClientId(String credentialsJson) {
        try {
            JsonNode json = objectMapper.readTree(credentialsJson);
            return json.get("installed").get("client_id").asText();
        } catch (Exception e) {
            logger.error("Error extracting client ID", e);
            return null;
        }
    }

    /**
     * Extract client secret from credentials JSON
     */
    private String getClientSecret(String credentialsJson) {
        try {
            JsonNode json = objectMapper.readTree(credentialsJson);
            return json.get("installed").get("client_secret").asText();
        } catch (Exception e) {
            logger.error("Error extracting client secret", e);
            return null;
        }
    }
}


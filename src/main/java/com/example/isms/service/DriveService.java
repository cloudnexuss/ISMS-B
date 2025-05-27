package com.example.isms.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.FileContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class DriveService {

    private static Drive getDriveService() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/main/resources/isms-file-store-6a965ee8bd7c.json"))
                .createScoped(Collections.singleton(DriveScopes.DRIVE));



        return new Drive.Builder(new NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("ISMS Drive")
                .build();
    }

    public static String uploadFile(java.io.File filePath, String folderId) throws Exception {
        Drive driveService = getDriveService();

        File fileMetadata = new File();
        fileMetadata.setName(filePath.getName());
        fileMetadata.setParents(Collections.singletonList("1Ro04anCJN4_v1QQA3JZMGmRJe4pBBfva")); // Drive folder ID

        FileContent mediaContent = new FileContent("application/octet-stream", filePath);

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        // Make file public
        driveService.permissions().create(file.getId(),
                        new com.google.api.services.drive.model.Permission()
                                .setType("anyone")
                                .setRole("reader"))
                .execute();

        return "https://drive.google.com/uc?id=" + file.getId();
    }


    public static String getSharableLinkByFileName(String fileName, String folderId) throws Exception {
        Drive driveService = getDriveService();

        String query = String.format("name = '%s' and '%s' in parents and trashed = false", fileName, folderId);

        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles().isEmpty()) {
            throw new Exception("File not found in Drive: " + fileName);
        }

        String fileId = result.getFiles().get(0).getId();

        // Make sure it's shared publicly (optional)
        driveService.permissions().create(fileId,
                        new com.google.api.services.drive.model.Permission()
                                .setType("anyone")
                                .setRole("reader"))
                .execute();

        return "https://drive.google.com/file/d/" + fileId + "/view";
    }

    public List<File> listFilesInFolder(String folderId) throws Exception {
        Drive driveService = getDriveService();

        String query = "'" + folderId + "' in parents and trashed = false";

        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, createdTime)")
                .execute();

        return result.getFiles();
    }


    public void deleteFile(String fileId) throws Exception {
        Drive driveService = getDriveService();
        driveService.files().delete(fileId).execute();
    }



}
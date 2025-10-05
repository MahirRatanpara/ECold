package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/resumes/{resumeId}
    private String userId;

    private String name;
    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private Boolean isDefault = false;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
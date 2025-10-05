package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @DocumentId
    private String id;

    private String email;
    private String name;
    private String password;
    private String profilePicture;
    private String provider; // Stored as String in Firestore
    private String providerId;
    private String accessToken;
    private String refreshToken;
    private Timestamp tokenExpiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Note: Firestore relationships are handled via subcollections
    // /users/{userId}/templates/
    // /users/{userId}/recruiters/
    // No need to store lists here

    public enum Provider {
        GOOGLE, MICROSOFT, LOCAL
    }

    // Helper method to convert enum to string
    public void setProviderEnum(Provider provider) {
        this.provider = provider != null ? provider.name() : null;
    }

    // Helper method to get enum from string
    public Provider getProviderEnum() {
        return this.provider != null ? Provider.valueOf(this.provider) : null;
    }
}
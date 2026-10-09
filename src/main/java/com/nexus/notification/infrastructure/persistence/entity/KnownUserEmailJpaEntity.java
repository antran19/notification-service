package com.nexus.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "known_user_emails")
public class KnownUserEmailJpaEntity {

    @Id
    private String userId;

    @Column(nullable = false)
    private String email;

    protected KnownUserEmailJpaEntity() {
    }

    public KnownUserEmailJpaEntity(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
}

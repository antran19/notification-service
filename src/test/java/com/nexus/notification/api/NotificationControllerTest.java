package com.nexus.notification.api;

import com.nexus.notification.api.mapper.NotificationApiMapperImpl;
import com.nexus.notification.application.usecase.ListAllNotificationsUseCase;
import com.nexus.notification.application.usecase.ListMyNotificationsUseCase;
import com.nexus.notification.application.usecase.MarkNotificationReadUseCase;
import com.nexus.notification.domain.model.Notification;
import com.nexus.notification.infrastructure.config.SecurityConfig;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.security.PrivilegeAuthorizationAspect;
import com.nexus.common.web.GlobalExceptionHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, NotificationApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, PrivilegeAuthorizationAspect.class})
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ListMyNotificationsUseCase listMyNotificationsUseCase;
    @MockBean private ListAllNotificationsUseCase listAllNotificationsUseCase;
    @MockBean private MarkNotificationReadUseCase markNotificationReadUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private void mockValidToken(String userId, List<String> privileges) {
        when(jwtTokenProvider.isValid("good-token")).thenReturn(true);
        Claims claims = Jwts.claims().subject(userId).add("privileges", privileges).build();
        when(jwtTokenProvider.parseClaims("good-token")).thenReturn(claims);
    }

    @Test
    void listMine_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMine_returns200WithOnlyCallersNotifications() throws Exception {
        mockValidToken("user-1", List.of());
        when(listMyNotificationsUseCase.list("user-1")).thenReturn(List.of(
                new Notification("n-1", "e-1", "UserRegistered", "user-1", "user-1", "Welcome!", "{}",
                        false, Instant.now(), Instant.now())));

        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recipientUserId").value("user-1"));
    }

    @Test
    void listAll_returns403WithoutAuditPrivilege() throws Exception {
        mockValidToken("user-1", List.of());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAll_returns200WithAuditPrivilege() throws Exception {
        mockValidToken("admin-1", List.of("NOTIFICATION.AUDIT"));
        when(listAllNotificationsUseCase.list(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void markRead_returns403WhenCallerDoesNotOwnTheNotification() throws Exception {
        mockValidToken("user-1", List.of());
        org.mockito.Mockito.doThrow(new ForbiddenException("NOT_YOUR_NOTIFICATION", "You do not own this notification"))
                .when(markNotificationReadUseCase).markRead(any(), any());

        mockMvc.perform(patch("/api/v1/notifications/n-1/read").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markRead_returns200WhenCallerOwnsTheNotification() throws Exception {
        mockValidToken("user-1", List.of());

        mockMvc.perform(patch("/api/v1/notifications/n-1/read").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }
}

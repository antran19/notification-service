package com.nexus.notification.application.service;

import com.nexus.common.events.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageResolverTest {

    private final NotificationMessageResolver resolver = new NotificationMessageResolver();

    @Test
    void resolve_userRegistered_returnsUserIdAndWelcomeMessage() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "alice@example.com", "Alice");

        NotificationMessageResolver.Resolution resolution = resolver.resolve("UserRegistered", event);

        assertThat(resolution.recipientUserId()).isEqualTo("user-1");
        assertThat(resolution.message()).contains("Alice");
    }

    @Test
    void resolve_bidPlaced_returnsBidderIdNotAggregateId() {
        BidPlacedEvent event = new BidPlacedEvent("auction-1", "bidder-1", new BigDecimal("150.00"));

        NotificationMessageResolver.Resolution resolution = resolver.resolve("BidPlaced", event);

        assertThat(resolution.recipientUserId()).isEqualTo("bidder-1");
        assertThat(resolution.message()).contains("150.00").contains("auction-1");
    }

    @Test
    void resolve_outbid_returnsOutbidBidderId() {
        OutbidEvent event = new OutbidEvent("auction-1", "outbid-bidder", new BigDecimal("200.00"));

        NotificationMessageResolver.Resolution resolution = resolver.resolve("Outbid", event);

        assertThat(resolution.recipientUserId()).isEqualTo("outbid-bidder");
    }

    @Test
    void resolve_auctionWon_returnsWinnerId() {
        AuctionWonEvent event = new AuctionWonEvent("auction-1", "product-1", "seller-1", "winner-1", new BigDecimal("500.00"));

        NotificationMessageResolver.Resolution resolution = resolver.resolve("AuctionWon", event);

        assertThat(resolution.recipientUserId()).isEqualTo("winner-1");
        assertThat(resolution.message()).contains("500.00");
    }

    @Test
    void resolve_auctionSettled_returnsWinnerId() {
        AuctionSettledEvent event = new AuctionSettledEvent("auction-1", "winner-1", new BigDecimal("500.00"), Instant.now());

        NotificationMessageResolver.Resolution resolution = resolver.resolve("AuctionSettled", event);

        assertThat(resolution.recipientUserId()).isEqualTo("winner-1");
    }

    @Test
    void resolve_unmappedEventType_returnsUnmapped() {
        NotificationMessageResolver.Resolution resolution = resolver.resolve("ProductCreated", null);

        assertThat(resolution).isEqualTo(NotificationMessageResolver.Resolution.UNMAPPED);
    }
}

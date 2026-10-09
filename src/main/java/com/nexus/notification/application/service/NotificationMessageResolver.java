package com.nexus.notification.application.service;

import com.nexus.common.events.*;

public class NotificationMessageResolver {

    public record Resolution(String recipientUserId, String message) {
        public static final Resolution UNMAPPED = new Resolution(null, null);
    }

    public Resolution resolve(String eventType, DomainEvent event) {
        return switch (eventType) {
            case "UserRegistered" -> resolveUserRegistered((UserRegisteredEvent) event);
            case "BidPlaced" -> resolveBidPlaced((BidPlacedEvent) event);
            case "Outbid" -> resolveOutbid((OutbidEvent) event);
            case "AuctionWon" -> resolveAuctionWon((AuctionWonEvent) event);
            case "AuctionSettled" -> resolveAuctionSettled((AuctionSettledEvent) event);
            case "PasswordResetRequested" -> resolvePasswordResetRequested((PasswordResetRequestedEvent) event);
            default -> Resolution.UNMAPPED;
        };
    }

    private Resolution resolvePasswordResetRequested(PasswordResetRequestedEvent e) {
        return new Resolution(e.getUserId(),
                "Mã đặt lại mật khẩu của bạn: " + e.getResetToken() + " (hết hạn sau 30 phút)");
    }

    private Resolution resolveUserRegistered(UserRegisteredEvent e) {
        return new Resolution(e.getUserId(), "Chào mừng " + e.getFullName() + " đã đăng ký tài khoản thành công!");
    }

    private Resolution resolveBidPlaced(BidPlacedEvent e) {
        return new Resolution(e.getBidderId(),
                "Bạn đã đặt giá " + e.getAmount() + " cho phiên đấu giá " + e.getAuctionId());
    }

    private Resolution resolveOutbid(OutbidEvent e) {
        return new Resolution(e.getOutbidBidderId(),
                "Bạn đã bị vượt giá trong phiên đấu giá " + e.getAuctionId()
                        + ", giá cao nhất hiện tại là " + e.getNewHighestBid());
    }

    private Resolution resolveAuctionWon(AuctionWonEvent e) {
        return new Resolution(e.getWinnerId(),
                "Chúc mừng! Bạn đã thắng phiên đấu giá " + e.getAuctionId() + " với giá " + e.getFinalPrice());
    }

    private Resolution resolveAuctionSettled(AuctionSettledEvent e) {
        return new Resolution(e.getWinnerId(),
                "Phiên đấu giá " + e.getAuctionId() + " đã hoàn tất thanh toán với giá " + e.getFinalPrice());
    }
}

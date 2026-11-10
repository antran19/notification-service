package com.nexus.notification.application.service;

import com.nexus.common.events.*;

public class NotificationMessageResolver {

    public record Resolution(String recipientUserId, String message) {
        public static final Resolution UNMAPPED = new Resolution(null, null);
        // The event was delivered before and already handled: there is nothing left to send.
        public static final Resolution ALREADY_RECORDED = new Resolution(null, null);
    }

    public Resolution resolve(String eventType, DomainEvent event) {
        return switch (eventType) {
            case "UserRegistered" -> resolveUserRegistered((UserRegisteredEvent) event);
            case "BidPlaced" -> resolveBidPlaced((BidPlacedEvent) event);
            case "Outbid" -> resolveOutbid((OutbidEvent) event);
            case "AuctionWon" -> resolveAuctionWon((AuctionWonEvent) event);
            case "AuctionSettled" -> resolveAuctionSettled((AuctionSettledEvent) event);
            case "AuctionPaymentTimeout" -> resolveAuctionPaymentTimeout((AuctionPaymentTimeoutEvent) event);
            case "OrderPaid" -> resolveOrderPaid((OrderPaidEvent) event);
            case "OrderShipped" -> resolveOrderShipped((OrderShippedEvent) event);
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

    // The winner did not pay in time. Told to the winner: the auction event carries no seller.
    private Resolution resolveAuctionPaymentTimeout(AuctionPaymentTimeoutEvent e) {
        return new Resolution(e.getWinnerId(),
                "Bạn đã quá hạn thanh toán phiên đấu giá " + e.getAuctionId() + " nên giao dịch không còn hiệu lực.");
    }

    // Told to the seller. The buyer already hears about a completed payment through AuctionSettled.
    private Resolution resolveOrderPaid(OrderPaidEvent e) {
        return new Resolution(e.getSellerId(),
                "Người mua đã thanh toán đơn hàng " + e.getOrderId() + " (phiên đấu giá " + e.getAuctionId()
                        + ") với số tiền " + e.getAmount() + ". Hãy chuẩn bị giao hàng.");
    }

    // Told to the buyer.
    private Resolution resolveOrderShipped(OrderShippedEvent e) {
        return new Resolution(e.getBuyerId(),
                "Đơn hàng " + e.getOrderId() + " (phiên đấu giá " + e.getAuctionId() + ") của bạn đã được giao đi.");
    }

    private Resolution resolveAuctionSettled(AuctionSettledEvent e) {
        return new Resolution(e.getWinnerId(),
                "Phiên đấu giá " + e.getAuctionId() + " đã hoàn tất thanh toán với giá " + e.getFinalPrice());
    }
}

package models;

public interface Discountable {
    double MEMBER_DISCOUNT      = 10.0;   // Regular: 10%
    double MEMBER_PLUS_DISCOUNT = 15.0;   // Plus   : 15%
    double calculateMemberDiscount(String memberTier);
    String getDiscountLabel();
}
package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    private String            transactionId;
    private LocalDateTime     dateTime;
    private Member            member;
    private List<TransactionItem> items;
    private double            totalBeforeDiscount;
    private double            totalAfterDiscount;
    private double            totalSavings;

    public Transaction() {
        this.transactionId       = null; // di-set oleh Repository
        this.dateTime            = LocalDateTime.now();
        this.items               = new ArrayList<>();
        this.totalBeforeDiscount = 0;
        this.totalAfterDiscount  = 0;
        this.totalSavings        = 0;
    }

    public Transaction(Member member) {
        this();
        this.member = member;
    }

    public void addItem(TransactionItem item) {
        items.add(item);
        recalculate();
    }

    public void addItemFromDb(TransactionItem item) {
        items.add(item);
    }

    private void recalculate() {
        totalBeforeDiscount = 0;
        totalAfterDiscount  = 0;
        totalSavings        = 0;
        for (TransactionItem item : items) {
            totalBeforeDiscount += item.getPriceAtTime() * item.getQty();
            totalAfterDiscount  += item.getSubtotal();
            totalSavings        += item.getSavings();
        }
    }

    public String getMemberTier() {
        if (member == null) return "NONE";
        return member.getTier().name();
    }

    public void setTotalsFromDb(double before, double savings, double after) {
        this.totalBeforeDiscount = before;
        this.totalSavings        = savings;
        this.totalAfterDiscount  = after;
    }

    public String               getTransactionId()           { return transactionId; }
    public void                 setTransactionId(String id)  { this.transactionId = id; } // ✅
    public LocalDateTime        getDateTime()                { return dateTime; }
    public Member               getMember()                  { return member; }
    public void                 setMember(Member m)          { this.member = m; }          // ✅
    public List<TransactionItem> getItems()                  { return items; }
    public double               getTotalBeforeDiscount()     { return totalBeforeDiscount; }
    public double               getTotalAfterDiscount()      { return totalAfterDiscount; }
    public double               getTotalSavings()            { return totalSavings; }

    public String getFormattedDate() {
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
}
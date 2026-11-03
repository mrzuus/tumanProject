package com.tuman.cash;

import java.util.List;

public final class History {
    private History() { }
    public static final class ReceiptRow {
        public String id, paidAt, tableId,tableName;
        public CashState.PaymentMethod method;
        public int total;
        public String refundId, refundedAt, reason;
        @Override public String toString() {
            return paidAt+" | стол "+tableId+" | "+total+" ₽ | "+(refundId==null ? "Оплачен" : "Возвращён");
        }
    }
    public static final class ReceiptDetail {
        public ReceiptRow receipt;
        public List<OrderItem> items;
    }
    public static final class Page {
        public final List<ReceiptRow> rows;
        public final boolean hasNext;
        public Page(List<ReceiptRow> rows,boolean hasNext) { this.rows=rows; this.hasNext=hasNext; }
    }
    public static final class AuditRow {
        public long id;
        public String at,actor,action,entityId,details;
        @Override public String toString() { return at+" | "+actor+" | "+action+"\n"+details; }
    }
    public static final class AuditPage {
        public final List<AuditRow> rows;
        public final boolean hasNext;
        public AuditPage(List<AuditRow> rows,boolean hasNext) { this.rows=rows; this.hasNext=hasNext; }
    }
}

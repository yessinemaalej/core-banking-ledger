package com.dev.bank.ledger;

import com.dev.bank.money.Money;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name = "journal_entry")
public class JournalEntry implements Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name="type")
    private JournalEntryType type;

    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private JournalEntryStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name ="reverses_entry_id")
    private UUID reversesEntryId;


    @Transient
    private boolean isNew = true;
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getId() { return id;}
    public JournalEntryStatus getStatus(){return status;}
    public JournalEntryType getType() {return type;}
    public LocalDate getBusinessDate(){return businessDate;}
    public LocalDate getValueDate(){return valueDate;}
    public UUID getReversesEntryId(){
        return reversesEntryId;
    }
    public String getCorrelationId(){
        return correlationId;
    }
    public Instant getCreatedAt(){ return createdAt;}
    public String getIdempotencyKey(){return idempotencyKey;}
    public String getCreatedBy(){return createdBy;}

    protected JournalEntry(){
        // For JPA
    }

    private JournalEntry(UUID id, JournalEntryType type, JournalEntryStatus status, Instant createdAt, LocalDate businessDate, String createdBy, LocalDate valueDate, String idempotencyKey, String correlationId, UUID reversesEntryId){
        this.id = id;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.businessDate = businessDate;
        this.valueDate = valueDate;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.reversesEntryId = reversesEntryId;
        this.createdBy = createdBy;
    }

    // package-private: outside ledger/, entries are created only through post(...), which checks the balance
    static JournalEntry transfer(LocalDate businessDate, LocalDate valueDate, String createdBy, String idempotencyKey, String correlationId){
        Objects.requireNonNull(businessDate, "businessDate is required for a transfer");
        Objects.requireNonNull(createdBy, "createdBy is required for a transfer");
        Objects.requireNonNull(valueDate, "valueDate is required for a transfer");

        UUID id = UUID.randomUUID();
        return new JournalEntry(id, JournalEntryType.TRANSFER, JournalEntryStatus.POSTED, Instant.now().truncatedTo(ChronoUnit.MICROS),businessDate,createdBy, valueDate, idempotencyKey, correlationId, null);

    }
    public static JournalEntry reversalOf(JournalEntry originalJournalEntry, String createdBy,LocalDate businessDate, String correlationId, String idempotencyKey){
        Objects.requireNonNull(originalJournalEntry, "originalJournalEntry is required for a reversal");
        Objects.requireNonNull(businessDate, "businessDate is required for a reversal");
        Objects.requireNonNull(createdBy, "createdBy is required for a reversal");


        UUID id = UUID.randomUUID();
        return new JournalEntry(id, JournalEntryType.REVERSAL, JournalEntryStatus.POSTED, Instant.now().truncatedTo(ChronoUnit.MICROS), businessDate, createdBy, originalJournalEntry.getValueDate(),idempotencyKey , correlationId, originalJournalEntry.getId());

    }

    public static void requireBalanced(List<Money> amounts) {
        if(amounts.size()<2){
            throw new UnbalancedJournalEntryException("A journal entry needs at least 2 postings, got " + amounts.size());
        }

        Map<String, Money> sumsMap = new HashMap<>();
        for (Money m: amounts){
            sumsMap.merge(m.currency(),m, Money::plus);
        }

        sumsMap.forEach((currency, total) -> {
            if (total.amountMinor() != 0) {
                throw new UnbalancedJournalEntryException(currency + " postings sum to " + total.amountMinor() + ", expected 0");
            }
        });
        
    }

    public static PostedEntry post(LocalDate businessDate, LocalDate valueDate, String createdBy,
                                   String idempotencyKey, String correlationId, List<PostingLine> lines) {
        Objects.requireNonNull(lines, "lines are required");
        requireBalanced(lines.stream().map(PostingLine::amount).toList());

        JournalEntry entry = transfer(businessDate, valueDate, createdBy, idempotencyKey, correlationId);
        List<Posting> postings = new ArrayList<>();
        for (PostingLine line : lines) {
            postings.add(Posting.of(entry, line.account(), line.amount()));
        }
        // unmodifiable, so no unchecked posting can be added after the balance check
        return new PostedEntry(entry, List.copyOf(postings));
    }
}

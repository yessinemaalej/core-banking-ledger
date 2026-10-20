package com.dev.bank.ledger;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

// no delete and no update: postings are immutable; errors are corrected by a reversing entry
public interface PostingRepository extends Repository<Posting, UUID> {
    void save(Posting posting);

    Optional<Posting> findById(UUID id);
}

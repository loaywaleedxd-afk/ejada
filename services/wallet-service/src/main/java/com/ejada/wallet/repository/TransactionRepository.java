package com.ejada.wallet.repository;

import com.ejada.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Optional<Transaction> findByReference(String reference);
}

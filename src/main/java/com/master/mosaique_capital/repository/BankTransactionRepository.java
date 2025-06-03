// com/master/mosaique_capital/repository/BankTransactionRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.BankAccount;
import com.master.mosaique_capital.entity.BankTransaction;
import com.master.mosaique_capital.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long>, JpaSpecificationExecutor<BankTransaction> {

    Optional<BankTransaction> findByAccountAndTransactionId(BankAccount account, String transactionId);

    Page<BankTransaction> findByAccountOrderByTransactionDateDesc(BankAccount account, Pageable pageable);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.account.connection.user = :user ORDER BY bt.transactionDate DESC")
    Page<BankTransaction> findByUserOrderByTransactionDateDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.account = :account AND bt.transactionDate BETWEEN :startDate AND :endDate ORDER BY bt.transactionDate DESC")
    List<BankTransaction> findByAccountAndDateRange(
            @Param("account") BankAccount account,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.account.connection.user = :user AND bt.transactionDate BETWEEN :startDate AND :endDate")
    List<BankTransaction> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT bt.category, SUM(ABS(bt.amount)) as total FROM BankTransaction bt " +
            "WHERE bt.account.connection.user = :user AND bt.amount < 0 " +
            "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY bt.category ORDER BY total DESC")
    List<CategoryExpenseProjection> getExpensesByCategory(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(bt.amount) FROM BankTransaction bt " +
            "WHERE bt.account.connection.user = :user AND bt.amount > 0 " +
            "AND bt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(ABS(bt.amount)) FROM BankTransaction bt " +
            "WHERE bt.account.connection.user = :user AND bt.amount < 0 " +
            "AND bt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(bt) FROM BankTransaction bt WHERE bt.account.connection.user = :user AND bt.category IS NULL")
    long countUncategorizedTransactionsByUser(@Param("user") User user);

    interface CategoryExpenseProjection {
        String getCategory();
        BigDecimal getTotal();
    }
}

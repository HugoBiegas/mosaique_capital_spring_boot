// com/master/mosaique_capital/repository/BankConnectionRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankConnectionRepository extends JpaRepository<BankConnection, Long> {

    List<BankConnection> findByUserOrderByCreatedAtDesc(User user);

    List<BankConnection> findByUserAndConnectionStatus(User user, String connectionStatus);

    Optional<BankConnection> findByUserAndConnectionId(User user, String connectionId);

    @Query("SELECT bc FROM BankConnection bc WHERE bc.user = :user AND bc.connectionStatus = 'ACTIVE'")
    List<BankConnection> findActiveConnectionsByUser(@Param("user") User user);

    @Query("SELECT bc FROM BankConnection bc WHERE bc.lastSyncAt < :threshold AND bc.connectionStatus = 'ACTIVE'")
    List<BankConnection> findConnectionsNeedingSync(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(bc) FROM BankConnection bc WHERE bc.user = :user AND bc.connectionStatus = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") String status);
}

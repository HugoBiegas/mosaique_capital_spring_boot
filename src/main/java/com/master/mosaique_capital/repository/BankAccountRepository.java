// com/master/mosaique_capital/repository/BankAccountRepository.java
package com.master.mosaique_capital.repository;

import com.master.mosaique_capital.entity.BankAccount;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByConnectionOrderByNameAsc(BankConnection connection);

    List<BankAccount> findByConnectionUserOrderByNameAsc(User user);

    Optional<BankAccount> findByConnectionAndAccountId(BankConnection connection, String accountId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.connection.user = :user AND ba.type = :type")
    List<BankAccount> findByUserAndType(@Param("user") User user, @Param("type") String type);

    @Query("SELECT SUM(ba.balance) FROM BankAccount ba WHERE ba.connection.user = :user")
    BigDecimal getTotalBalanceByUser(@Param("user") User user);

    @Query("SELECT SUM(ba.balance) FROM BankAccount ba WHERE ba.connection.user = :user AND ba.balance > 0")
    BigDecimal getTotalAssetsbyUser(@Param("user") User user);

    @Query("SELECT SUM(ABS(ba.balance)) FROM BankAccount ba WHERE ba.connection.user = :user AND ba.balance < 0")
    BigDecimal getTotalLiabilitiesByUser(@Param("user") User user);

    @Query("SELECT ba.type as type, SUM(ba.balance) as total FROM BankAccount ba WHERE ba.connection.user = :user GROUP BY ba.type")
    List<BalanceByTypeProjection> getBalanceByType(@Param("user") User user);

    interface BalanceByTypeProjection {
        String getType();
        BigDecimal getTotal();
    }
}

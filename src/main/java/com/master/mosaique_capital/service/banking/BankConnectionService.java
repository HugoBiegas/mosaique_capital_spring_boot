// com/master/mosaique_capital/service/banking/BankConnectionService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.dto.banking.BankConnectionDto;
import com.master.mosaique_capital.dto.banking.BankConnectionRequest;
import com.master.mosaique_capital.dto.banking.BankSyncResponse;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.exception.BankConnectionException;
import com.master.mosaique_capital.mapper.BankConnectionMapper;
import com.master.mosaique_capital.repository.BankConnectionRepository;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.service.banking.external.BankAggregationService;
import com.master.mosaique_capital.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des connexions bancaires
 * Intègre l'agrégation de comptes via des providers externes (Budget Insight, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankConnectionService {

    private final BankConnectionRepository bankConnectionRepository;
    private final UserRepository userRepository;
    private final BankConnectionMapper bankConnectionMapper;
    private final BankAggregationService bankAggregationService;
    private final BankAccountSyncService bankAccountSyncService;

    /**
     * Récupère toutes les connexions bancaires de l'utilisateur connecté
     */
    @Transactional(readOnly = true)
    public List<BankConnectionDto> getAllConnections() {
        User currentUser = getCurrentUser();
        List<BankConnection> connections = bankConnectionRepository.findByUserOrderByCreatedAtDesc(currentUser);
        return bankConnectionMapper.toDtoList(connections);
    }

    /**
     * Récupère une connexion bancaire par ID avec vérification d'ownership
     */
    @Transactional(readOnly = true)
    public BankConnectionDto getConnectionById(Long id) {
        BankConnection connection = findConnectionById(id);
        checkConnectionOwnership(connection);
        return bankConnectionMapper.toDto(connection);
    }

    /**
     * Initie une nouvelle connexion bancaire
     */
    @Transactional
    public BankConnectionDto initiateConnection(BankConnectionRequest request) {
        User currentUser = getCurrentUser();

        log.info("Initiation d'une nouvelle connexion bancaire pour l'utilisateur {} avec le provider {}",
                currentUser.getUsername(), request.getProvider());

        try {
            // Validation du provider
            if (!bankAggregationService.isSupportedProvider(request.getProvider())) {
                throw new BankConnectionException("Provider non supporté: " + request.getProvider());
            }

            // Création de la connexion via le service d'agrégation
            String externalConnectionId = bankAggregationService.initiateConnection(
                    request.getProvider(),
                    request.getBankCredentials()
            );

            // Sauvegarde en base
            BankConnection connection = new BankConnection();
            connection.setUser(currentUser);
            connection.setProvider(request.getProvider());
            connection.setConnectionId(externalConnectionId);
            connection.setConnectionStatus("PENDING");

            BankConnection savedConnection = bankConnectionRepository.save(connection);

            log.info("Connexion bancaire créée avec succès. ID: {}, External ID: {}",
                    savedConnection.getId(), externalConnectionId);

            return bankConnectionMapper.toDto(savedConnection);

        } catch (Exception e) {
            log.error("Erreur lors de l'initiation de la connexion bancaire: {}", e.getMessage(), e);
            throw new BankConnectionException("Impossible d'établir la connexion bancaire: " + e.getMessage());
        }
    }

    /**
     * Confirme une connexion bancaire (après authentification forte côté banque)
     */
    @Transactional
    public BankConnectionDto confirmConnection(Long connectionId, String confirmationCode) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        log.info("Confirmation de la connexion bancaire ID: {}", connectionId);

        try {
            // Confirmation via le service d'agrégation
            boolean confirmed = bankAggregationService.confirmConnection(
                    connection.getConnectionId(),
                    confirmationCode
            );

            if (confirmed) {
                connection.setConnectionStatus("ACTIVE");
                connection.setLastSyncAt(LocalDateTime.now());

                // Synchronisation initiale des comptes
                bankAccountSyncService.syncAccountsForConnection(connection);

                BankConnection savedConnection = bankConnectionRepository.save(connection);

                log.info("Connexion bancaire confirmée et comptes synchronisés. ID: {}", connectionId);
                return bankConnectionMapper.toDto(savedConnection);
            } else {
                throw new BankConnectionException("Code de confirmation invalide");
            }

        } catch (Exception e) {
            log.error("Erreur lors de la confirmation de la connexion bancaire: {}", e.getMessage(), e);
            connection.setConnectionStatus("ERROR");
            bankConnectionRepository.save(connection);
            throw new BankConnectionException("Erreur de confirmation: " + e.getMessage());
        }
    }

    /**
     * Synchronise les données d'une connexion bancaire
     */
    @Transactional
    public BankSyncResponse synchronizeConnection(Long connectionId) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        if (!"ACTIVE".equals(connection.getConnectionStatus())) {
            throw new BankConnectionException("La connexion n'est pas active. Statut: " + connection.getConnectionStatus());
        }

        log.info("Démarrage de la synchronisation pour la connexion ID: {}", connectionId);

        try {
            BankSyncResponse syncResponse = bankAccountSyncService.syncAccountsForConnection(connection);

            connection.setLastSyncAt(LocalDateTime.now());
            bankConnectionRepository.save(connection);

            log.info("Synchronisation terminée pour la connexion ID: {}. Comptes: {}, Transactions: {}",
                    connectionId, syncResponse.getAccountsSynced(), syncResponse.getTransactionsSynced());

            return syncResponse;

        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de la connexion bancaire: {}", e.getMessage(), e);
            throw new BankConnectionException("Erreur de synchronisation: " + e.getMessage());
        }
    }

    /**
     * Supprime une connexion bancaire et toutes ses données associées
     */
    @Transactional
    public void deleteConnection(Long connectionId) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        log.info("Suppression de la connexion bancaire ID: {}", connectionId);

        try {
            // Révocation côté provider externe
            bankAggregationService.revokeConnection(connection.getConnectionId());

            // Suppression en base (cascade sur comptes et transactions)
            bankConnectionRepository.delete(connection);

            log.info("Connexion bancaire supprimée avec succès. ID: {}", connectionId);

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la connexion bancaire: {}", e.getMessage(), e);
            throw new BankConnectionException("Erreur de suppression: " + e.getMessage());
        }
    }

    /**
     * Met à jour le statut d'une connexion bancaire
     */
    @Transactional
    public BankConnectionDto updateConnectionStatus(Long connectionId, String status) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        log.info("Mise à jour du statut de la connexion ID: {} vers {}", connectionId, status);

        connection.setConnectionStatus(status);
        BankConnection savedConnection = bankConnectionRepository.save(connection);

        return bankConnectionMapper.toDto(savedConnection);
    }

    /**
     * Vérifie l'état de santé d'une connexion bancaire
     */
    @Transactional(readOnly = true)
    public boolean isConnectionHealthy(Long connectionId) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        try {
            return bankAggregationService.checkConnectionHealth(connection.getConnectionId());
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de la santé de la connexion: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Force la resynchronisation de toutes les connexions actives d'un utilisateur
     */
    @Transactional
    public List<BankSyncResponse> resyncAllActiveConnections() {
        User currentUser = getCurrentUser();
        List<BankConnection> activeConnections = bankConnectionRepository.findByUserAndConnectionStatus(currentUser, "ACTIVE");

        return activeConnections.stream()
                .map(connection -> {
                    try {
                        return bankAccountSyncService.syncAccountsForConnection(connection);
                    } catch (Exception e) {
                        log.error("Erreur lors de la synchronisation de la connexion {}: {}",
                                connection.getId(), e.getMessage());
                        return BankSyncResponse.error(connection.getId(), e.getMessage());
                    }
                })
                .toList();
    }

    // ===== Méthodes utilitaires =====

    private BankConnection findConnectionById(Long id) {
        return bankConnectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connexion bancaire non trouvée avec l'ID: " + id));
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private void checkConnectionOwnership(BankConnection connection) {
        User currentUser = getCurrentUser();
        if (!connection.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à cette connexion bancaire");
        }
    }
}
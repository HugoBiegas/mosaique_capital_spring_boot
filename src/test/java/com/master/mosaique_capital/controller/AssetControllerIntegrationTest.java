// src/test/java/com/master/mosaique_capital/controller/AssetControllerIntegrationTest.java
package com.master.mosaique_capital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetType;
import com.master.mosaique_capital.enums.Role;
import com.master.mosaique_capital.repository.AssetRepository;
import com.master.mosaique_capital.repository.AssetTypeRepository;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AssetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetTypeRepository assetTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser;
    private AssetTypeEntity testAssetType;
    private Asset testAsset;
    private String jwtToken;
    private AssetCreateRequest assetCreateRequest;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données
        assetRepository.deleteAll();
        userRepository.deleteAll();

        // Récupérer ou créer le type d'actif REAL_ESTATE
        testAssetType = assetTypeRepository.findByCode(AssetType.REAL_ESTATE.name())
                .orElseGet(() -> {
                    AssetTypeEntity newType = new AssetTypeEntity();
                    newType.setCode(AssetType.REAL_ESTATE.name());
                    newType.setLabel(AssetType.REAL_ESTATE.getLabel());
                    newType.setDescription("Biens immobiliers");
                    return assetTypeRepository.save(newType);
                });

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        testUser.setRoles(Collections.singleton(Role.ROLE_USER));
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        // Créer un actif de test
        testAsset = new Asset();
        testAsset.setName("Appartement Paris");
        testAsset.setDescription("Appartement dans le 15ème arrondissement");
        testAsset.setType(testAssetType);
        testAsset.setOwner(testUser);
        testAsset.setCurrentValue(new BigDecimal("450000"));
        testAsset.setCurrency("EUR");
        testAsset = assetRepository.save(testAsset);

        // Générer un token JWT pour les tests
        jwtToken = "Bearer " + tokenProvider.createToken("testuser");

        // Initialiser la requête de création d'actif
        assetCreateRequest = new AssetCreateRequest();
        assetCreateRequest.setName("Appartement Lyon");
        assetCreateRequest.setDescription("Appartement dans le centre ville");
        assetCreateRequest.setType(AssetType.REAL_ESTATE);
        assetCreateRequest.setCurrentValue(new BigDecimal("350000"));
        assetCreateRequest.setCurrency("EUR");
    }

    @Test
    void getAllAssets_ShouldReturnAssets_WhenUserIsAuthenticated() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/assets")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier la réponse
        List<AssetDto> assets = objectMapper.readValue(result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, AssetDto.class));

        assertFalse(assets.isEmpty());
        assertEquals(1, assets.size());
        assertEquals("Appartement Paris", assets.get(0).getName());
    }

    @Test
    void getAssetById_ShouldReturnAsset_WhenAssetExistsAndUserIsOwner() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/assets/{id}", testAsset.getId())
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier la réponse
        AssetDto asset = objectMapper.readValue(result.getResponse().getContentAsString(), AssetDto.class);

        assertEquals(testAsset.getId(), asset.getId());
        assertEquals(testAsset.getName(), asset.getName());
        assertEquals(testAsset.getDescription(), asset.getDescription());
        assertEquals(AssetType.REAL_ESTATE, asset.getType());
    }

    @Test
    void getAssetById_ShouldReturnNotFound_WhenAssetDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/assets/{id}", 999L)
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAsset_ShouldCreateAndReturnAsset_WhenInputIsValid() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/assets")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetCreateRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Vérifier la réponse
        AssetDto createdAsset = objectMapper.readValue(result.getResponse().getContentAsString(), AssetDto.class);

        assertNotNull(createdAsset.getId());
        assertEquals(assetCreateRequest.getName(), createdAsset.getName());
        assertEquals(assetCreateRequest.getDescription(), createdAsset.getDescription());
        assertEquals(assetCreateRequest.getType(), createdAsset.getType());

        // Vérifier que l'actif a été enregistré en base de données
        assertTrue(assetRepository.existsById(createdAsset.getId()));
    }

    @Test
    void updateAsset_ShouldUpdateAndReturnAsset_WhenInputIsValidAndUserIsOwner() throws Exception {
        // Given
        AssetDto updateRequest = new AssetDto();
        updateRequest.setName("Appartement Paris - Mis à jour");
        updateRequest.setDescription("Appartement rénové dans le 15ème arrondissement");
        updateRequest.setCurrentValue(new BigDecimal("480000"));
        updateRequest.setType(AssetType.REAL_ESTATE);
        updateRequest.setCurrency("EUR");

        // When & Then
        MvcResult result = mockMvc.perform(put("/api/assets/{id}", testAsset.getId())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier la réponse
        AssetDto updatedAsset = objectMapper.readValue(result.getResponse().getContentAsString(), AssetDto.class);

        assertEquals(testAsset.getId(), updatedAsset.getId());
        assertEquals(updateRequest.getName(), updatedAsset.getName());
        assertEquals(updateRequest.getDescription(), updatedAsset.getDescription());
        assertEquals(updateRequest.getCurrentValue(), updatedAsset.getCurrentValue());

        // Vérifier que l'actif a été mis à jour en base de données
        Asset assetInDb = assetRepository.findById(testAsset.getId()).orElse(null);
        assertNotNull(assetInDb);
        assertEquals(updateRequest.getName(), assetInDb.getName());
    }

    @Test
    void deleteAsset_ShouldDeleteAsset_WhenAssetExistsAndUserIsOwner() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/assets/{id}", testAsset.getId())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNoContent());

        // Vérifier que l'actif a été supprimé
        assertFalse(assetRepository.existsById(testAsset.getId()));
    }
}
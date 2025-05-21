// src/test/java/com/master/mosaique_capital/service/AssetServiceTest.java
package com.master.mosaique_capital.service;

import com.master.mosaique_capital.dto.asset.AssetCreateRequest;
import com.master.mosaique_capital.dto.asset.AssetDto;
import com.master.mosaique_capital.entity.Asset;
import com.master.mosaique_capital.entity.AssetTypeEntity;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.AssetType;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.mapper.AssetMapper;
import com.master.mosaique_capital.repository.AssetRepository;
import com.master.mosaique_capital.repository.AssetTypeRepository;
import com.master.mosaique_capital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetTypeRepository assetTypeRepository;

    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetService assetService;

    private User testUser;
    private Asset testAsset;
    private AssetTypeEntity testAssetType;
    private AssetDto testAssetDto;
    private AssetCreateRequest testAssetCreateRequest;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Initialiser l'utilisateur de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Initialiser le type d'actif
        testAssetType = new AssetTypeEntity();
        testAssetType.setId(1L);
        testAssetType.setCode(AssetType.REAL_ESTATE.name());
        testAssetType.setLabel(AssetType.REAL_ESTATE.getLabel());

        // Initialiser l'actif de test
        testAsset = new Asset();
        testAsset.setId(1L);
        testAsset.setName("Appartement Paris");
        testAsset.setDescription("Appartement dans le 15ème arrondissement");
        testAsset.setType(testAssetType);
        testAsset.setOwner(testUser);
        testAsset.setCurrentValue(new BigDecimal("450000"));
        testAsset.setCurrency("EUR");
        testAsset.setCreatedAt(LocalDateTime.now());
        testAsset.setUpdatedAt(LocalDateTime.now());

        // Initialiser le DTO de l'actif
        testAssetDto = new AssetDto();
        testAssetDto.setId(1L);
        testAssetDto.setName("Appartement Paris");
        testAssetDto.setDescription("Appartement dans le 15ème arrondissement");
        testAssetDto.setType(AssetType.REAL_ESTATE);
        testAssetDto.setCurrentValue(new BigDecimal("450000"));
        testAssetDto.setCurrency("EUR");
        testAssetDto.setCreatedAt(testAsset.getCreatedAt());
        testAssetDto.setUpdatedAt(testAsset.getUpdatedAt());

        // Initialiser la requête de création d'actif
        testAssetCreateRequest = new AssetCreateRequest();
        testAssetCreateRequest.setName("Appartement Paris");
        testAssetCreateRequest.setDescription("Appartement dans le 15ème arrondissement");
        testAssetCreateRequest.setType(AssetType.REAL_ESTATE);
        testAssetCreateRequest.setCurrentValue(new BigDecimal("450000"));
        testAssetCreateRequest.setCurrency("EUR");

        // Configurer l'authentification
        authentication = new UsernamePasswordAuthenticationToken("testuser", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getAllAssets_ShouldReturnListOfAssets() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(assetRepository.findByOwner(any(User.class))).thenReturn(Arrays.asList(testAsset));
        when(assetMapper.toDtoList(anyList())).thenReturn(Arrays.asList(testAssetDto));

        // When
        List<AssetDto> result = assetService.getAllAssets();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAssetDto.getId(), result.get(0).getId());
        verify(assetRepository, times(1)).findByOwner(testUser);
        verify(assetMapper, times(1)).toDtoList(anyList());
    }

    @Test
    void getAssetById_ShouldReturnAsset_WhenAssetExists() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(assetRepository.findById(anyLong())).thenReturn(Optional.of(testAsset));
        when(assetMapper.toDto(any(Asset.class))).thenReturn(testAssetDto);

        // When
        AssetDto result = assetService.getAssetById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testAsset.getId(), result.getId());
        verify(assetRepository, times(1)).findById(1L);
        verify(assetMapper, times(1)).toDto(testAsset);
    }

    @Test
    void getAssetById_ShouldThrowException_WhenAssetDoesNotExist() {
        // Given
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> assetService.getAssetById(99L));
        verify(assetRepository, times(1)).findById(99L);
    }


    @Test
    void getAssetById_ShouldThrowException_WhenUserDoesNotOwnAsset() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");

        Asset assetWithAnotherOwner = new Asset();
        assetWithAnotherOwner.setId(1L);
        assetWithAnotherOwner.setOwner(anotherUser);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(assetRepository.findById(anyLong())).thenReturn(Optional.of(assetWithAnotherOwner));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> assetService.getAssetById(1L));
        verify(assetRepository, times(1)).findById(1L);
    }

    @Test
    void createAsset_ShouldCreateAndReturnAsset() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(assetTypeRepository.findByCode(anyString())).thenReturn(Optional.of(testAssetType));
        when(assetMapper.toEntity(any(AssetCreateRequest.class))).thenReturn(testAsset);
        when(assetRepository.save(any(Asset.class))).thenReturn(testAsset);
        when(assetMapper.toDto(any(Asset.class))).thenReturn(testAssetDto);

        // When
        AssetDto result = assetService.createAsset(testAssetCreateRequest);

        // Then
        assertNotNull(result);
        assertEquals(testAssetDto.getId(), result.getId());
        assertEquals(testAssetDto.getName(), result.getName());
        verify(assetRepository, times(1)).save(testAsset);
        verify(assetMapper, times(1)).toDto(testAsset);
    }

    @Test
    void deleteAsset_ShouldDeleteAsset_WhenAssetExistsAndUserIsOwner() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(assetRepository.findById(anyLong())).thenReturn(Optional.of(testAsset));
        doNothing().when(assetRepository).delete(any(Asset.class));

        // When
        assetService.deleteAsset(1L);

        // Then
        verify(assetRepository, times(1)).delete(testAsset);
    }
}
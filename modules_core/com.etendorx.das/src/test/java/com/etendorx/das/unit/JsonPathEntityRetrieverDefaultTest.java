package com.etendorx.das.unit;

import com.etendorx.entities.mapper.lib.JsonPathEntityRetrieverDefault;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This class contains unit tests for the JsonPathEntityRetrieverDefault class.
 */
public class JsonPathEntityRetrieverDefaultTest {

    // Mocked JpaSpecificationExecutor repository
    private JpaSpecificationExecutor<Car> repository;
    // Instance of the class under test
    private JsonPathEntityRetrieverDefault<Car> retriever;

    /**
     * Inner static class representing a Car entity.
     */
    @Data
    @AllArgsConstructor
    static class Car {
        String id;
        String name;
    }

    /**
     * This method sets up the test environment before each test.
     */
    @BeforeEach
    public void setup() {
        // Mock the JpaSpecificationExecutor repository
        repository = mock(JpaSpecificationExecutor.class);
        // Initialize the JsonPathEntityRetrieverDefault with the mocked repository
        retriever = new JsonPathEntityRetrieverDefault<>(repository);
    }

    /**
     * This test verifies that the getRepository method returns the provided repository.
     */
    @Test
    public void getRepositoryReturnsProvidedRepository() {
        assertEquals(repository, retriever.getRepository());
    }

    /**
     * This test verifies that the getKeys method returns an empty array.
     */
    @Test
    public void getKeysReturnsEmptyArray() {
        String[] keys = retriever.getKeys();
        assertNotNull(keys);
        assertEquals(0, keys.length);
    }

    /**
     * This test verifies that the getRepository method always returns the same repository.
     */
    @Test
    public void getRepositoryReturnsSameRepository() {
        JpaSpecificationExecutor<Car> sameRepository = retriever.getRepository();
        assertSame(repository, sameRepository);
    }

    /**
     * This test verifies that the getKeys method returns a new array each time it is called.
     */
    @Test
    public void getKeysReturnsNewArrayEachTime() {
        String[] keys1 = retriever.getKeys();
        String[] keys2 = retriever.getKeys();
        assertNotSame(keys1, keys2);
    }
}

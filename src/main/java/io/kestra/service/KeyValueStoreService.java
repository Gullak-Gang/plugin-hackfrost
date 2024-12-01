package io.kestra.service;

import java.io.IOException;
import io.kestra.core.exceptions.ResourceExpiredException;
import io.kestra.core.runners.RunContext;
import io.kestra.core.storages.kv.KVValueAndMetadata;

public class KeyValueStoreService {
    private final RunContext runContext;
    private final String namespace;

    public KeyValueStoreService(RunContext runContext) {
        this.runContext = runContext;
        this.namespace = runContext.flowInfo().namespace();
    }

    /**
     * Add a key-value pair to the store
     *
     * @param key   The key to store
     * @param value The value to store
     * @throws IOException          If there's a network or request error
     * @throws InterruptedException If the request is interrupted
     */
    public void addKeyValue(String key, String value) throws IOException, InterruptedException {
        runContext.namespaceKv(namespace).put(key, new KVValueAndMetadata(null, value), true);
    }

    /**
     * Retrieve a value for a given key
     *
     * @param key The key to retrieve
     * @return The value associated with the key, or null if not found
     * @throws IOException          If there's a network or request error
     * @throws ResourceExpiredException If the resource is expired
     */
    public String getKeyValue(String key) throws IOException, ResourceExpiredException {
        if (runContext.namespaceKv(namespace).getValue(key).isEmpty()) {
            return null;
        }
        return (String) runContext.namespaceKv(namespace).getValue(key).get().value();
    }

    /**
     * Delete a key-value pair from the store
     *
     * @param key The key to delete
     * @return true if the key was successfully deleted, false if the key was not
     *         found
     * @throws IOException          If there's a network or request error
     */
    public boolean deleteKeyValue(String key) throws IOException {
        return runContext.namespaceKv(namespace).delete(key);
    }
}
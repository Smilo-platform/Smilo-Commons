/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.db;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Generic Store interface, which could be implemented by different databases. Used to separate database specific logic
 * from the application logic.
 */
public interface Store {

    /**
     * Store a value in the database. Updates the existing entry by key if it already exists in the database.
     * @param collection collection to save to
     * @param key identifier of the entity
     * @param value the entity to store
     */
    void put(String collection, byte[] key, byte[] value);

    /**
     * Retrieves an entity from the database by key
     * @param collection collection to retrieve from
     * @param key key to query for
     * @return the entity
     */
    byte[] get(String collection, byte[] key);

    /**
     * Removes an entity from the database by key
     * @param collection collection to remove from
     * @param key key to query for
     * @return the entity
     */
    boolean remove(String collection, byte[] key);

    /**
     * Retrieves all entities from the specified collection
     * @param collection collection to retrieve from
     * @return A list of entities
     */
    Map<byte[], byte[]> getAll(String collection);

    /**
     * Retrieves last entity of the specified collection
     * @param collection collection to retrieve from
     * @return the last entity of the collection
     */
    Map.Entry<byte[], byte[]> last(String collection);

    /**
     * Initializes the collection, must be called after the application startup for every collection
     * @param collectionName collection to initialize
     */
    void initializeCollection(String collectionName);

    /**
     * Clears all entities in a collection
     * @param collectionName collection to clear
     */
    void clear(String collectionName);

    /**
     * Retrieves the amount of entities in a collection
     * @param collectionName collection to query
     * @return amount of entities
     */
    Long getEntries(String collectionName);




    /**************** BEGIN ONLY USED BY API ************************/
    long getArrayLength(String collection, String key);
    void addToArray(String collection, String key, ByteBuffer value);
    List<byte[]> getArray(String collection, String key, long skip, long take, boolean isDescending);
    List<byte[]> getAllAPI(String collection, long skip, long take, boolean isDescending);
    byte[] getAPI(String collection, ByteBuffer key);

    /**************** END ONLY USED BY API ************************/


}

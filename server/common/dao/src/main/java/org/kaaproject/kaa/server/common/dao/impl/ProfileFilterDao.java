/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.server.common.dao.impl;

import java.util.List;

/**
 * The interface Profile filter dao.
 *
 * @param <T> the type parameter
 */
public interface ProfileFilterDao<T> extends SqlDao<T> {

    /**
     * Find all profile filters by profile schema id
     *
     * @param profileSchemaId the profile schema id
     * @return the list of profile schemas
     */
    List<T> findAllByProfileSchemaId(String profileSchemaId);

    /**
     * Find actual profile filters by endpoint group id.
     *
     * @param endpointGroupId the endpoint group id
     * @return the list of profile filters
     */
    List<T> findActualByEndpointGroupId(String endpointGroupId);

    /**
     * Find actual profile filters by group id and schema id.
     *
     * @param schemaId the schema id
     * @param groupId the group id
     * @return the list of profile filters
     */
    List<T> findActualBySchemaIdAndGroupId(String schemaId, String groupId);

    /**
     * Find latest deprecated filter for endpoint group and profile schema.
     *
     * @param schemaId the schema id
     * @param groupId  the group id
     * @return the profile filter
     */
    T findLatestDeprecated(String schemaId, String groupId);

    /**
     * Remove profile filters by endpoint group id
     *
     * @param endpointGroupId the endpoint group id
     */
    void removeByEndpointGroupId(String endpointGroupId);

    /**
     * Find profile filters by application and schema version.
     *
     * @param appId the application id
     * @param schemaVersion the schema version
     * @return the list of profile filters
     */
    List<T> findByAppIdAndSchemaVersion(String appId, int schemaVersion);

    /**
     * Find inactive profile filters.
     *
     * @param schemaId the schema id
     * @param groupId  the group id
     * @return the profile filter
     */
    T findInactiveFilter(String schemaId, String groupId);

    /**
     * Find latest filter for endpoint group and profile schema.
     *
     * @param schemaId the schema id
     * @param groupId  the group id
     * @return the profile filter
     */
    T findLatestFilter(String schemaId, String groupId);

    /**
     * Activate profile filter by profile filter id.
     *
     * @param id the id
     * @param activatedUsername the name of user that activated profile filter
     * @return the profile filter
     */
    T activate(String id, String activatedUsername);

    /**
     * Deactivate profile filter by id.
     *
     * @param id the id
     * @param deactivatedUsername the name of user that deactivated profile filter
     * @return the profile filter
     */
    T deactivate(String id, String deactivatedUsername);

    /**
     * Deactivate old profile filters by profile schema and endpoint group ids.
     *
     * @param schemaId the schema id
     * @param groupId  the group id
     * @param deactivatedUsername the name of user that deactivated profile filter
     * @return deactivated profile filter
     */
    T deactivateOldFilter(String schemaId, String groupId, String deactivatedUsername);

    /**
     * This method get count of active filters by schema id and group id.
     * @param schemaId the profile schema id
     * @param groupId the endpoint group id
     * @return count of filter
     */
    long findActiveFilterCount(String schemaId, String groupId);
}

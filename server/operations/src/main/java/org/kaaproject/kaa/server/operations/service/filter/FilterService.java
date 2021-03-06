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

package org.kaaproject.kaa.server.operations.service.filter;

import java.util.List;

import org.kaaproject.kaa.common.dto.ProfileFilterDto;
import org.kaaproject.kaa.server.operations.service.cache.AppVersionKey;


/**
 * Service that is responsible for creation and house-keeping of various profile {@link Filter filters}.
 *
 * @author ashvayka
 */
public interface FilterService {

    /**
     * Gets all matching filters by key and body.
     *
     * @param appProfileVersionKey the key that is build from AppToken and profile version. 
     * @param profileBody the profile body
     * @return the all matching filters
     */
    List<ProfileFilterDto> getAllMatchingFilters(AppVersionKey appProfileVersionKey, String profileBody);

    /**
     * Check if profileBody matches filter that is represented by filter id.
     *
     * @param appToken the app token
     * @param profileFilterId the profile filter id
     * @param profileBody the profile body
     * @return true, if successful
     */
    boolean matches(String appToken, String profileFilterId, String profileBody);

}

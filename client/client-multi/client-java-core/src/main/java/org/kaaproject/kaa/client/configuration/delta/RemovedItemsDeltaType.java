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

package org.kaaproject.kaa.client.configuration.delta;

import java.util.LinkedList;
import java.util.List;

/**
 * Field with this delta type removes items by their handler ids ({@link DeltaType} implementation)
 *
 * @author Yaroslav Zeygerman
 *
 */
public class RemovedItemsDeltaType extends EmptyDeltaType {
    private List<DeltaHandlerId> removedItems = new LinkedList<DeltaHandlerId>();

    public RemovedItemsDeltaType() {

    }

    void addHandlerId(DeltaHandlerId handlerId) {
        this.removedItems.add(handlerId);
    }

    @Override
    public List<DeltaHandlerId> getRemovedItems() {
        return removedItems;
    }
}

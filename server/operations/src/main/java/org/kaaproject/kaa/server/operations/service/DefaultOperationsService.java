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

package org.kaaproject.kaa.server.operations.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kaaproject.kaa.common.dto.EndpointGroupStateDto;
import org.kaaproject.kaa.common.dto.EndpointProfileDto;
import org.kaaproject.kaa.common.dto.EndpointUserConfigurationDto;
import org.kaaproject.kaa.common.dto.NotificationDto;
import org.kaaproject.kaa.common.dto.TopicDto;
import org.kaaproject.kaa.common.endpoint.security.MessageEncoderDecoder;
import org.kaaproject.kaa.common.hash.EndpointObjectHash;
import org.kaaproject.kaa.common.hash.SHA1HashUtils;
import org.kaaproject.kaa.server.common.Base64Util;
import org.kaaproject.kaa.server.common.dao.UserConfigurationService;
import org.kaaproject.kaa.server.operations.pojo.GetDeltaRequest;
import org.kaaproject.kaa.server.operations.pojo.GetDeltaResponse;
import org.kaaproject.kaa.server.operations.pojo.GetNotificationRequest;
import org.kaaproject.kaa.server.operations.pojo.GetNotificationResponse;
import org.kaaproject.kaa.server.operations.pojo.RegisterProfileRequest;
import org.kaaproject.kaa.server.operations.pojo.SyncContext;
import org.kaaproject.kaa.server.operations.pojo.UpdateProfileRequest;
import org.kaaproject.kaa.server.operations.pojo.exceptions.GetDeltaException;
import org.kaaproject.kaa.server.operations.service.cache.AppSeqNumber;
import org.kaaproject.kaa.server.operations.service.cache.CacheService;
import org.kaaproject.kaa.server.operations.service.cache.HistorySubject;
import org.kaaproject.kaa.server.operations.service.delta.DeltaService;
import org.kaaproject.kaa.server.operations.service.delta.HistoryDelta;
import org.kaaproject.kaa.server.operations.service.history.HistoryDeltaService;
import org.kaaproject.kaa.server.operations.service.notification.NotificationDeltaService;
import org.kaaproject.kaa.server.operations.service.profile.ProfileService;
import org.kaaproject.kaa.server.operations.service.user.EndpointUserService;
import org.kaaproject.kaa.server.sync.ClientSyncMetaData;
import org.kaaproject.kaa.server.sync.ConfigurationClientSync;
import org.kaaproject.kaa.server.sync.ConfigurationServerSync;
import org.kaaproject.kaa.server.sync.EndpointAttachRequest;
import org.kaaproject.kaa.server.sync.EndpointAttachResponse;
import org.kaaproject.kaa.server.sync.EndpointDetachRequest;
import org.kaaproject.kaa.server.sync.EndpointDetachResponse;
import org.kaaproject.kaa.server.sync.EventClientSync;
import org.kaaproject.kaa.server.sync.EventListenersRequest;
import org.kaaproject.kaa.server.sync.EventListenersResponse;
import org.kaaproject.kaa.server.sync.EventServerSync;
import org.kaaproject.kaa.server.sync.Notification;
import org.kaaproject.kaa.server.sync.NotificationClientSync;
import org.kaaproject.kaa.server.sync.NotificationServerSync;
import org.kaaproject.kaa.server.sync.NotificationType;
import org.kaaproject.kaa.server.sync.ProfileClientSync;
import org.kaaproject.kaa.server.sync.ProfileServerSync;
import org.kaaproject.kaa.server.sync.ServerSync;
import org.kaaproject.kaa.server.sync.SubscriptionType;
import org.kaaproject.kaa.server.sync.SyncResponseStatus;
import org.kaaproject.kaa.server.sync.SyncStatus;
import org.kaaproject.kaa.server.sync.Topic;
import org.kaaproject.kaa.server.sync.UserClientSync;
import org.kaaproject.kaa.server.sync.UserServerSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class DefaultOperationsService.
 */
@Service
public class DefaultOperationsService implements OperationsService {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultOperationsService.class);

    /** The delta service. */
    @Autowired
    DeltaService deltaService;

    /** The profile service. */
    @Autowired
    ProfileService profileService;

    /** The cache service. */
    @Autowired
    CacheService cacheService;

    /** The delta service. */
    @Autowired
    HistoryDeltaService historyDeltaService;

    /** The notification delta service. */
    @Autowired
    NotificationDeltaService notificationDeltaService;

    @Autowired
    EndpointUserService endpointUserService;

    @Autowired
    UserConfigurationService userConfigurationService;

    private String operationServerHash;

    @Override
    public SyncContext syncProfile(SyncContext context, ProfileClientSync profileSyncRequest) {
        EndpointProfileDto profile = context.getEndpointProfile();

        ClientSyncMetaData metaData = context.getMetaData();
        if (profileSyncRequest != null) {
            ProfileServerSync profileSyncResponse;
            if (profileSyncRequest.getEndpointPublicKey() != null) {
                LOG.debug("[{}][{}] registration of endpoint started.", context.getEndpointKey(), context.getRequestHash());
                profile = registerEndpoint(context.getEndpointKey(), context.getRequestHash(), metaData, profileSyncRequest);
            } else {
                LOG.debug("[{}][{}] update of endpoint profile started.", context.getEndpointKey(), context.getRequestHash());
                profile = updateEndpoint(context.getEndpointKey(), context.getRequestHash(), metaData, profileSyncRequest);
            }
            profileSyncResponse = new ProfileServerSync(SyncResponseStatus.DELTA);
            metaData.setProfileHash(ByteBuffer.wrap(profile.getProfileHash()));
            context.setProfileSyncResponse(profileSyncResponse);
        }

        if (profile == null) {
            LOG.debug("[{}][{}] fetching profile.", context.getEndpointKey(), context.getRequestHash());
            EndpointObjectHash endpointHash = EndpointObjectHash.fromBytes(metaData.getEndpointPublicKeyHash().array());
            profile = profileService.getProfile(endpointHash);
            LOG.trace("[{}][{}] fetched profile {}.", context.getEndpointKey(), context.getRequestHash(), profile);
        }

        context.setEndpointProfile(profile);

        if (!Arrays.equals(profile.getProfileHash(), toByteArray(metaData.getProfileHash()))) {
            LOG.debug("[{}] Profile hash mismatch. Profile resync needed", context.getEndpointKey());
            if(LOG.isTraceEnabled()){
                LOG.trace("[{}] persisted profile hash is {}", context.getEndpointKey(), MessageEncoderDecoder.bytesToHex(profile.getProfileHash()));
                LOG.trace("[{}] client profile hash is {}", context.getEndpointKey(),  MessageEncoderDecoder.bytesToHex(toByteArray(metaData.getProfileHash())));
            }
            context.setStatus(SyncStatus.PROFILE_RESYNC);
        }

        return context;
    }

    @Override
    public SyncContext processEndpointAttachDetachRequests(SyncContext context, UserClientSync request) {
        if (request != null) {
            LOG.trace("[{}][{}] procesing user sync request {}.", context.getEndpointKey(), context.getRequestHash(), request);
            UserServerSync userSyncResponse = processUserSyncRequest(context.getEndpointKey(), context.getRequestHash(), request,
                    context.getEndpointProfile());
            context.setUserSyncResponse(userSyncResponse);
        }
        return context;
    }

    @Override
    public SyncContext processEventListenerRequests(SyncContext context, EventClientSync request) {
        if (request != null) {
            ClientSyncMetaData metaData = context.getMetaData();
            LOG.trace("[{}][{}] procesing event sync request {}.", context.getEndpointKey(), context.getRequestHash(), request);
            EventServerSync eventSyncResponse = processEventSyncResponse(context.getEndpointKey(), context.getRequestHash(),
                    metaData.getApplicationToken(), request, context.getEndpointProfile());
            context.setEventSyncResponse(eventSyncResponse);
        }
        return context;
    }

    @Override
    public SyncContext syncConfiguration(SyncContext context, ConfigurationClientSync request) throws GetDeltaException {
        if (request != null) {
            EndpointProfileDto profile = context.getEndpointProfile();
            ClientSyncMetaData md = context.getMetaData();
            AppSeqNumber appSeqNumber = cacheService.getAppSeqNumber(md.getApplicationToken());
            if (context.getAppSeqNumber() == null) {
                context.setAppSeqNumber(appSeqNumber);
            }
            int curAppSeqNumber = appSeqNumber.getSeqNumber();
            LOG.debug("[{}][{}] fetched app seq number {}", context.getEndpointKey(), context.getRequestHash(), curAppSeqNumber);
            LOG.trace("[{}][{}] procesing configuration sync request {}.", context.getEndpointKey(), context.getRequestHash(), request);
            LOG.debug("[{}][{}] fetching history for seq numbers {}-{}", context.getEndpointKey(), context.getRequestHash(),
                    request.getAppStateSeqNumber(), curAppSeqNumber);
            int startSeqNumber = Math.min(request.getAppStateSeqNumber(), profile.getCfSequenceNumber());
            LOG.debug("[{}][{}] calculating configuration delta using seq number {}", context.getEndpointKey(), context.getRequestHash(),
                    startSeqNumber);
            request.setAppStateSeqNumber(startSeqNumber);
            HistoryDelta historyDelta = fetchHistory(context.getEndpointKey(), context.getRequestHash(), md.getApplicationToken(), profile,
                    HistorySubject.CONFIGURATION, startSeqNumber, curAppSeqNumber);
            GetDeltaResponse confResponse = calculateConfigurationDelta(md, request, context, historyDelta, curAppSeqNumber);
            ConfigurationServerSync confSyncResponse = buildConfSyncResponse(confResponse, curAppSeqNumber);
            context.setConfigurationSyncResponse(confSyncResponse);

            if (historyDelta.isSmthChanged() || SyncResponseStatus.NO_DELTA != confSyncResponse.getResponseStatus()) {
                List<EndpointGroupStateDto> endpointGroups = historyDelta.getEndpointGroupStates();
                LOG.debug("[{}][{}] Updating profile with endpoint groups.size {}, groups: {}", context.getEndpointKey(),
                        context.getRequestHash(), endpointGroups.size(), endpointGroups);
                profile.setCfGroupStates(endpointGroups);
                profile.setCfSequenceNumber(curAppSeqNumber);
                context.setUpdateProfileRequired(true);
            }
        }
        return context;
    }

    @Override
    public SyncContext syncNotification(SyncContext context, NotificationClientSync request) {
        if (request != null) {
            EndpointProfileDto profile = context.getEndpointProfile();
            ClientSyncMetaData md = context.getMetaData();
            AppSeqNumber appSeqNumber = cacheService.getAppSeqNumber(md.getApplicationToken());
            if (context.getAppSeqNumber() == null) {
                context.setAppSeqNumber(appSeqNumber);
            }
            int curAppSeqNumber = appSeqNumber.getSeqNumber();

            LOG.trace("[{}][{}] procesing notification sync request {}.", context.getEndpointKey(), context.getRequestHash(), request);
            LOG.debug("[{}][{}] fetching history for seq numbers {}-{}", context.getEndpointKey(), context.getRequestHash(),
                    request.getAppStateSeqNumber(), curAppSeqNumber);
            int startSeqNumber = Math.min(request.getAppStateSeqNumber(), profile.getNfSequenceNumber());
            LOG.debug("[{}][{}] calculating notification delta using seq number {}", context.getEndpointKey(), context.getRequestHash(),
                    startSeqNumber);
            HistoryDelta historyDelta = fetchHistory(context.getEndpointKey(), context.getRequestHash(), md.getApplicationToken(), profile,
                    HistorySubject.NOTIFICATION, startSeqNumber, curAppSeqNumber);
            GetNotificationResponse notificationResponse = calculateNotificationDelta(request, profile, historyDelta);
            context.setSubscriptionStates(notificationResponse.getSubscriptionStates());
            NotificationServerSync nfSyncResponse = buildNotificationSyncResponse(notificationResponse, curAppSeqNumber);
            context.setNotificationSyncResponse(nfSyncResponse);

            context.setUpdateProfileRequired(context.isUpdateProfileRequired() || notificationResponse.isSubscriptionListChanged());
            if (historyDelta.isSmthChanged() || SyncResponseStatus.NO_DELTA != nfSyncResponse.getResponseStatus()) {
                List<EndpointGroupStateDto> endpointGroups = historyDelta.getEndpointGroupStates();
                LOG.debug("[{}][{}] Updating profile with endpoint groups.size {}, groups: {}", context.getEndpointKey(),
                        context.getRequestHash(), endpointGroups.size(), endpointGroups);
                profile.setNfGroupStates(endpointGroups);
                profile.setNfSequenceNumber(curAppSeqNumber);
                context.setUpdateProfileRequired(true);
            }
        }
        return context;
    }

    /**
     * Process sync.
     */
    @Override
    public EndpointProfileDto updateProfile(SyncContext context) {
        EndpointProfileDto profile = context.getEndpointProfile();
        if (!operationServerHash.equals(profile.getServerHash())) {
            LOG.debug("[{}] Operations server hash changed from {} to {}", context.getEndpointKey(), profile.getServerHash(),
                    operationServerHash);
            profile.setServerHash(operationServerHash);
            context.setUpdateProfileRequired(true);
        }

        if (context.isUpdateProfileRequired()) {
            context.setEndpointProfile(profileService.updateProfile(profile));
        }
        return context.getEndpointProfile();
    }

    @Override
    public byte[] fetchUcfHash(String appToken, EndpointProfileDto profile) {
        if (profile.getEndpointUserId() == null || profile.getEndpointUserId().isEmpty()) {
            return null;
        }
        EndpointUserConfigurationDto ucfDto = userConfigurationService.findUserConfigurationByUserIdAndAppTokenAndSchemaVersion(
                profile.getEndpointUserId(), appToken, profile.getConfigurationVersion());
        if (ucfDto != null) {
            return EndpointObjectHash.fromString(ucfDto.getBody()).getData();
        }
        return null;
    }

    private EndpointProfileDto registerEndpoint(String endpointId, int requestHash, ClientSyncMetaData metaData, ProfileClientSync request) {
        LOG.debug("[{}][{}] register endpoint. request: {}", endpointId, requestHash, request);
        byte[] endpointKey = toByteArray(request.getEndpointPublicKey());
        byte[] profileBody = toByteArray(request.getProfileBody());

        RegisterProfileRequest registerProfileRequest = new RegisterProfileRequest(metaData.getApplicationToken(), endpointKey,
                request.getVersionInfo(), profileBody, request.getEndpointAccessToken());
        EndpointProfileDto endpointProfile = profileService.registerProfile(registerProfileRequest);
        LOG.debug("profile registered. id: {}, endpointKeyHash: {}", endpointProfile.getId(), endpointProfile.getEndpointKeyHash());
        return endpointProfile;
    }

    private EndpointProfileDto updateEndpoint(String endpointId, int requestHash, ClientSyncMetaData metaData, ProfileClientSync request) {
        LOG.debug("[{}][{}] update endpoint. request: {}", endpointId, requestHash, request);
        EndpointObjectHash endpointKeyHash = EndpointObjectHash.fromBytes(toByteArray(metaData.getEndpointPublicKeyHash()));
        UpdateProfileRequest updateRequest = new UpdateProfileRequest(metaData.getApplicationToken(), endpointKeyHash,
                request.getEndpointAccessToken(), request.getProfileBody().array(), request.getVersionInfo());
        EndpointProfileDto endpointProfile = profileService.updateProfile(updateRequest);
        LOG.debug("profile updated. id: {}, endpointKeyHash: {}", endpointProfile.getId(), endpointProfile.getEndpointKeyHash());
        return endpointProfile;
    }

    private EventServerSync processEventSyncResponse(String endpointId, int requestHash, String appToken, EventClientSync request,
            EndpointProfileDto profile) {
        EventServerSync response = new EventServerSync();
        List<EventListenersRequest> requests = request.getEventListenersRequests();
        if (requests != null && !requests.isEmpty()) {
            LOG.debug("[{}] processing {} endpoint detach requests", endpointId, requests.size());
            List<EventListenersResponse> responses = new ArrayList<>(requests.size());
            for (EventListenersRequest elRequest : requests) {
                LOG.debug("[{}] processing event listener request {}", endpointId, request);
                EventListenersResponse elResponse = endpointUserService.findListeners(profile, appToken, elRequest);
                LOG.debug("[{}] event listener response {}", endpointId, response);
                responses.add(elResponse);
            }
            response.setEventListenersResponses(responses);
        } else {
            List<EventListenersResponse> emptyList = Collections.emptyList();
            response.setEventListenersResponses(emptyList);
        }
        return response;
    }

    private UserServerSync processUserSyncRequest(String endpointId, int requestHash, UserClientSync request, EndpointProfileDto profile) {
        UserServerSync response = new UserServerSync();
        if (request.getEndpointAttachRequests() != null) {
            response.setEndpointAttachResponses(processEndpointAttachRequests(endpointId, requestHash, request, profile));
        }
        if (request.getEndpointDetachRequests() != null) {
            response.setEndpointDetachResponses(processEndpointDetachRequests(endpointId, requestHash, request, profile));
        }
        return response;
    }

    private List<EndpointAttachResponse> processEndpointAttachRequests(String endpointId, int requestHash, UserClientSync syncRequest,
            EndpointProfileDto profile) {
        List<EndpointAttachRequest> requests = syncRequest.getEndpointAttachRequests();
        if (requests != null && !requests.isEmpty()) {
            LOG.debug("[{}][{}] processing {} endpoint attach requests", endpointId, requestHash, requests.size());
            List<EndpointAttachResponse> responses = new ArrayList<>(requests.size());
            for (EndpointAttachRequest request : syncRequest.getEndpointAttachRequests()) {
                LOG.debug("[{}][{}] processing endpoint attach request {}", endpointId, requestHash, request);
                EndpointAttachResponse response = endpointUserService.attachEndpoint(profile, request);
                LOG.debug("[{}][{}] endpoint attach response {}", endpointId, requestHash, response);
                responses.add(response);
            }
            return responses;
        } else {
            return Collections.emptyList();
        }
    }

    private List<EndpointDetachResponse> processEndpointDetachRequests(String endpointId, int requestHash, UserClientSync syncRequest,
            EndpointProfileDto profile) {
        List<EndpointDetachRequest> requests = syncRequest.getEndpointDetachRequests();
        if (requests != null && !requests.isEmpty()) {
            LOG.debug("[{}] processing {} endpoint detach requests", endpointId, requests.size());
            List<EndpointDetachResponse> responses = new ArrayList<>(requests.size());
            for (EndpointDetachRequest request : requests) {
                LOG.debug("[{}] processing endpoint detach request {}", endpointId, request);
                EndpointDetachResponse response = endpointUserService.detachEndpoint(profile, request);
                LOG.debug("[{}] endpoint detach response {}", endpointId, response);
                responses.add(response);
            }
            return responses;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Builds the notification sync response.
     *
     * @param notificationResponse
     *            the notification response
     * @return the notification sync response
     */
    private static NotificationServerSync buildNotificationSyncResponse(GetNotificationResponse notificationResponse, int curAppSeqNumber) {
        NotificationServerSync response = new NotificationServerSync();
        response.setResponseStatus(SyncResponseStatus.NO_DELTA);

        if (notificationResponse.getNotifications() != null) {
            List<Notification> notifications = new ArrayList<Notification>();
            for (NotificationDto notificationDto : notificationResponse.getNotifications()) {
                notifications.add(convertNotification(notificationDto));
            }
            response.setNotifications(notifications);
        }

        if (notificationResponse.getTopicList() != null) {
            List<Topic> topicList = new ArrayList<Topic>();
            for (TopicDto topicDto : notificationResponse.getTopicList()) {
                Topic topic = new Topic();
                topic.setId(topicDto.getId());
                topic.setName(topicDto.getName());
                switch (topicDto.getType()) {
                case MANDATORY:
                    topic.setSubscriptionType(SubscriptionType.MANDATORY);
                    break;
                case OPTIONAL:
                    topic.setSubscriptionType(SubscriptionType.OPTIONAL);
                    break;
                default:
                    break;
                }
                topicList.add(topic);
            }
            response.setAvailableTopics(topicList);
        }

        if (notificationResponse.hasDelta()) {
            response.setResponseStatus(SyncResponseStatus.DELTA);
        }

        response.setAppStateSeqNumber(curAppSeqNumber);
        return response;
    }

    /**
     * Convert notification.
     *
     * @param notificationDto
     *            the notification dto
     * @return the notification
     */
    private static Notification convertNotification(NotificationDto notificationDto) {
        Notification notification = new Notification();
        notification.setBody(ByteBuffer.wrap(notificationDto.getBody()));
        notification.setTopicId(notificationDto.getTopicId());
        switch (notificationDto.getType()) {
        case SYSTEM:
            notification.setType(NotificationType.SYSTEM);
            break;
        case USER:
            notification.setType(NotificationType.CUSTOM);
            break;
        default:
            break;
        }
        if (notificationDto.getSecNum() >= 0) {
            notification.setSeqNumber(notificationDto.getSecNum());
        } else {
            // unicast notification
            notification.setUid(notificationDto.getId());
        }
        return notification;
    }

    /**
     * Builds the conf sync response.
     *
     * @param deltaResponse
     *            the conf response
     * @return the conf sync response
     * @throws GetDeltaException
     *             the get delta exception
     */
    private static ConfigurationServerSync buildConfSyncResponse(GetDeltaResponse deltaResponse, int curAppSeqNumber)
            throws GetDeltaException {
        ConfigurationServerSync response = new ConfigurationServerSync();
        if (deltaResponse.getDelta() != null) {
            try {
                response.setConfDeltaBody(ByteBuffer.wrap(deltaResponse.getDelta().getData()));
            } catch (IOException e) {
                LOG.error("conf delta invalid: {}", e);
                throw new GetDeltaException(e);
            }
        }
        if (deltaResponse.getConfSchema() != null) {
            try {
                response.setConfSchemaBody(ByteBuffer.wrap(deltaResponse.getConfSchema().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                LOG.error("conf schema invalid: {}", e);
                throw new GetDeltaException(e);
            }
        }
        switch (deltaResponse.getResponseType()) {
        case CONF_RESYNC:
            response.setResponseStatus(SyncResponseStatus.RESYNC);
            break;
        case DELTA:
            response.setResponseStatus(SyncResponseStatus.DELTA);
            break;
        default:
            response.setResponseStatus(SyncResponseStatus.NO_DELTA);
            break;
        }
        response.setAppStateSeqNumber(curAppSeqNumber);
        return response;
    }

    /**
     * Calculate notification delta.
     *
     * @param syncRequest
     *            the sync request
     * @param profile
     *            the profile
     * @param historyDelta
     *            the history delta
     * @return the gets the notification response
     */
    private GetNotificationResponse calculateNotificationDelta(NotificationClientSync syncRequest, EndpointProfileDto profile,
            HistoryDelta historyDelta) {
        GetNotificationRequest request = new GetNotificationRequest(profile, syncRequest.getSubscriptionCommands(),
                syncRequest.getAcceptedUnicastNotifications(), syncRequest.getTopicStates());
        return notificationDeltaService.getNotificationDelta(request, historyDelta);
    }

    /**
     * Calculate configuration delta.
     *
     * @param request
     *            the request
     * @param profile
     *            the profile
     * @param historyDelta
     *            the history delta
     * @param curAppSeqNumber
     *            the cur app seq number
     * @param fetchSchema
     *            the fetch schema
     * @return the gets the delta response
     * @throws GetDeltaException
     *             the get delta exception
     */
    private GetDeltaResponse calculateConfigurationDelta(ClientSyncMetaData metaData, ConfigurationClientSync request, SyncContext context,
            HistoryDelta historyDelta, int curAppSeqNumber) throws GetDeltaException {
        GetDeltaRequest deltaRequest;
        if (request.getConfigurationHash() != null) {
            deltaRequest = new GetDeltaRequest(metaData.getApplicationToken(), EndpointObjectHash.fromBytes(request.getConfigurationHash()
                    .array()), request.getAppStateSeqNumber(), request.isResyncOnly());
        } else {
            deltaRequest = new GetDeltaRequest(metaData.getApplicationToken(), request.getAppStateSeqNumber());
        }
        deltaRequest.setEndpointProfile(context.getEndpointProfile());
        deltaRequest.setUserConfigurationChanged(context.isUserConfigurationChanged());
        deltaRequest.setUserConfigurationHash(context.getUserConfigurationHash());
        return deltaService.getDelta(deltaRequest, historyDelta, curAppSeqNumber);
    }

    /**
     * Fetch history.
     *
     * @param request
     *            the request
     * @param profile
     *            the profile
     * @param curAppSeqNumber
     *            the cur app seq number
     * @return the history delta
     */
    private HistoryDelta fetchHistory(String endpointId, int requesHash, String applicationToken, EndpointProfileDto profile,
            HistorySubject subject, int startSeqNumber, int endSeqNumber) {
        if (isFirstRequest(profile, subject)) {
            LOG.debug("[{}] Profile has no endpoint groups yet. calculating full list", endpointId);
            return historyDeltaService.getDelta(profile, applicationToken, endSeqNumber);
        } else {
            LOG.debug("[{}] Profile has endpoint groups. Calculating changes", endpointId);
            return historyDeltaService.getDelta(profile, subject, applicationToken, startSeqNumber, endSeqNumber);
        }
    }

    /**
     * Checks if is first request.
     *
     * @param request
     *            the request
     * @return true, if is first request
     */
    public static boolean isFirstRequest(EndpointProfileDto profile, HistorySubject subject) {
        if(subject == HistorySubject.CONFIGURATION){
            return profile.getCfGroupStates() == null || profile.getCfGroupStates().size() == 0;
        }else{
            return profile.getNfGroupStates() == null || profile.getNfGroupStates().size() == 0;
        }
    }

    /**
     * To byte array.
     *
     * @param buffer
     *            the buffer
     * @return the byte[]
     */
    private static byte[] toByteArray(ByteBuffer buffer) {
        return Arrays.copyOf(buffer.array(), buffer.array().length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.kaaproject.kaa.server.operations.service.OperationsService#
     * updateSyncResponse(org.kaaproject.kaa.common.endpoint.gen.SyncResponse,
     * java.util.List, java.lang.String)
     */
    @Override
    public ServerSync updateSyncResponse(ServerSync response, List<NotificationDto> notificationDtos, String unicastNotificationId) {
        LOG.debug("Updating sync response {}", response);
        boolean modified = false;
        NotificationServerSync notificationResponse = response.getNotificationSync();
        if (notificationResponse == null) {
            notificationResponse = new NotificationServerSync();
            response.setNotificationSync(notificationResponse);
        }

        List<Notification> notifications = notificationResponse.getNotifications();
        if (notifications == null) {
            notifications = new ArrayList<Notification>();
        }
        for (NotificationDto notificationDto : notificationDtos) {
            Notification newNotification = convertNotification(notificationDto);
            boolean found = false;
            for (Notification oldNotification : notifications) {
                if (oldNotification.getSeqNumber() == newNotification.getSeqNumber()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                modified = true;
                notifications.add(newNotification);
            } else {
                LOG.debug("Notification with seq number {} is already present in response", newNotification.getSeqNumber());
            }
        }
        if (unicastNotificationId != null) {
            boolean found = false;
            for (Notification oldNotification : notifications) {
                if (oldNotification.getUid() != null && oldNotification.getUid().equals(unicastNotificationId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                modified = true;
                NotificationDto unicast = notificationDeltaService.findUnicastNotificationById(unicastNotificationId);
                notifications.add(convertNotification(unicast));
            } else {
                LOG.debug("Notification with uid [{}] is already present in response", unicastNotificationId);
            }
        }

        if (modified) {
            notificationResponse.setNotifications(notifications);
            notificationResponse.setResponseStatus(SyncResponseStatus.DELTA);
            LOG.debug("Updated sync response {}", response);
            return response;
        }
        LOG.debug("Sync response was not updated!");
        return null;
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        operationServerHash = Base64Util.encode(SHA1HashUtils.hashToBytes(publicKey.getEncoded()));
    }

    @Override
    public EndpointProfileDto attachEndpointToUser(EndpointProfileDto profile, String appToken, String userExternalId) {
        return endpointUserService.attachEndpointToUser(profile, appToken, userExternalId);
    }
}

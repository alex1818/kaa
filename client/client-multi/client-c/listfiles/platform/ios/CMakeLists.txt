#
# Copyright 2014-2015 CyberVision, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#



set(KAA_SOURCE_FILES 
        ${KAA_SOURCE_FILES}
        ${KAA_SRC_FOLDER}/platform-impl/ios/sha.c
        ${KAA_SRC_FOLDER}/platform-impl/posix/logger.c
        ${KAA_SRC_FOLDER}/platform-impl/ext_log_storage_memory.c
        ${KAA_SRC_FOLDER}/platform-impl/ext_log_upload_strategy_by_volume.c
    )

if(NOT KAA_WITHOUT_TCP_CHANNEL)
    set(KAA_SOURCE_FILES 
            ${KAA_SOURCE_FILES}
            ${KAA_SRC_FOLDER}/kaa_protocols/kaa_tcp/kaatcp_parser.c
            ${KAA_SRC_FOLDER}/kaa_protocols/kaa_tcp/kaatcp_request.c
            ${KAA_SRC_FOLDER}/platform-impl/posix/posix_tcp_utils.c
            ${KAA_SRC_FOLDER}/platform-impl/kaa_tcp_channel.c
        )
endif()

set(KAA_THIRDPARTY_LIBRARIES
        ${KAA_THIRDPARTY_LIBRARIES} 
    )

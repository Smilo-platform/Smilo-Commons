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

package io.smilo.commons.peer.payloadhandler;

public enum PayloadType {

    READY_TO_LINK,
    NETWORK_LIST,
    LINK_NETWORK,
    LINK_ACCEPTED,
    LINK_REJECTED,
    READY_FULL_NODE,
    FULL_NODE_ACCEPTED,
    FULL_NODE_DECLINED,
    DISCONNECT,
    NETWORK_STATE,
    REQUEST_NET_STATE,
    REQUEST_IDENTIFIER,
    RESPOND_IDENTIFIER,
    TRANSACTION,
    MESSAGE,
    PEER,
    GET_PEER,
    COMMIT,
    APPROVE,
    DECLINE,
    SPORT_CHALLENGE,
    SPORT_RESPOND,
    NEW_SPEAKER,
    SPEAKER_ACCEPTED,
    SECONDARY_SPEAKER,
    BLOCK,
    GET_BLOCK,
    PING,
    PONG
}

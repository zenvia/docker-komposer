package com.zenvia.komposer.model.docker

/**
 * Created by klein on 17/07/15.
 */
class NetworkSettings {
    def String ipAddress;
    def Integer ipPrefixLen;
    def String gateway;
    def String bridge;
    def Map<String, Map<String, String>> portMapping;
    def Map<String, List<PortBinding>> ports;
    def String macAddress;
}

package com.zenvia.komposer.model.docker

/**
 * Created by klein on 17/07/15.
 */
class ContainerInfo {
    def String id;
    def Date created;
    def String path;
    def List<String> args;
    def ContainerConfig config;
    def HostConfig hostConfig;
    def ContainerState state;
    def String image;
    def NetworkSettings networkSettings;
    def String resolvConfPath;
    def String hostnamePath;
    def String hostsPath;
    def String name;
    def String driver;
    def String execDriver;
    def String processLabel;
    def String mountLabel;
    def Map<String, String> volumes;
    def Map<String, Boolean> volumesRW;
}

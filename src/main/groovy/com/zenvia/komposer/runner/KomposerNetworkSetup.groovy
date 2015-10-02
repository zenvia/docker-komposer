package com.zenvia.komposer.runner

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.LogStream
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import groovy.util.logging.Slf4j

import java.nio.charset.StandardCharsets

/**
 * Created by tiagooliveira on 7/29/15.
 */
@Slf4j
class KomposerNetworkSetup {

    private static final String WEAVE_IMAGE_TAG = 'weaveworks/weaveexec:1.0.1'

    def extractHost = { dockerClient ->
        return dockerClient.uri.authority.split(':').first()
    }

    def status(dockerClient) {
        def container = createNetworkAdminContainer('status', extractHost(dockerClient))
        def status = startAdminContainer(container, dockerClient)
        return status
    }

    def start(dockerClient) {

        this.pullWeaveImage(dockerClient)

        ['launch', 'launch-dns', 'launch-proxy'].each {
            def container = createNetworkAdminContainer(it, extractHost(dockerClient))
            startAdminContainer(container, dockerClient)
        }
    }

    def pullWeaveImage(dockerClient) {
        if (!this.checkIfWeaveImageIsPresent(dockerClient)) {
            log.info("Pulling weave image!")
            dockerClient.pull(WEAVE_IMAGE_TAG)
        }
    }

    def checkIfWeaveImageIsPresent(dockerClient) {
        return dockerClient.listImages().findAll { it.repoTags.toString().contains(WEAVE_IMAGE_TAG) }
    }

    def stop(dockerClient) {
        ['stop', 'stop-dns', 'stop-proxy'].each {
            def container = createNetworkAdminContainer(it, extractHost(dockerClient))
            startAdminContainer(container, dockerClient)
        }
    }

    def getHost(dockerClient) {
        def container = createNetworkAdminContainer('proxy-env', extractHost(dockerClient))
        def logs = startAdminContainer(container, dockerClient)
        def hostMatcher = (logs.split('\n').last() =~ /localhost:\d{1,5}|\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d{1,5}/)
        def host
        if (hostMatcher.size() > 0) {
            host = hostMatcher[0]
        }
        return host
    }

    def startAdminContainer(container, dockerClient) {
        def creation = dockerClient.createContainer(container)
        dockerClient.startContainer(creation.id)

        def result = ''
        LogStream logs = dockerClient.logs(creation.id, DockerClient.LogsParameter.FOLLOW,
                DockerClient.LogsParameter.STDOUT,
                DockerClient.LogsParameter.STDERR,
                DockerClient.LogsParameter.TIMESTAMPS)

        while (logs.hasNext()) {
            def logLine = StandardCharsets.US_ASCII.decode(logs.next().content()).toString()
            log.debug(logLine)
            result += logLine
        }

        this.safeKillContainer(dockerClient, creation.id)
        this.safeRemoveContainer(dockerClient, creation.id)

        return result
    }

    def safeKillContainer(dockerClient, containerId) {
        try {
            dockerClient.killContainer(containerId)
        } catch (Exception e) {
            log.info("Container $containerId already killed!")
        }
    }

    def safeRemoveContainer(dockerClient, containerId) {
        try {
            dockerClient.removeContainer(containerId)
        } catch (Exception e) {
            log.info("Container $containerId already removed!")
        }
    }

    def createNetworkAdminContainer(command, host, debug = '') {
        def hostConfig = HostConfig.builder().privileged(true).binds('/var/run/docker.sock:/var/run/docker.sock', '/proc:/hostproc').networkMode('host').build()

        def containerConfig = ContainerConfig.builder()
                .image(WEAVE_IMAGE_TAG)
                .hostConfig(hostConfig)
                .cmd('--local', command)
                .env('PROCFS=/hostproc',
                'DOCKERHUB_USER=weaveworks',
                'VERSION',
                "WEAVE_DEBUG=$debug",
                'WEAVE_DOCKER_ARGS',
                'WEAVEDNS_DOCKER_ARGS',
                'WEAVEPROXY_DOCKER_ARGS',
                'WEAVE_PASSWORD',
                'WEAVE_PORT',
                'WEAVE_CONTAINER_NAME=weave',
                'DOCKER_BRIDGE',
                "PROXY_HOST=$host",
                'WEAVE_CIDR=none')
                .build()

        return containerConfig
    }
}

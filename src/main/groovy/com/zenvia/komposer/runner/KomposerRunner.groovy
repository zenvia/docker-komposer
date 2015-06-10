package com.zenvia.komposer.runner

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerCertificates
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.LogMessage
import com.spotify.docker.client.LogStream
import com.zenvia.komposer.builder.KomposerBuilder
import groovy.util.logging.Log
import com.spotify.docker.client.DockerClient.LogsParameter

import java.nio.file.Paths

import static com.google.common.base.Charsets.UTF_8

/**
 * @author Tiago Oliveira
 * @todo make possible to run one single container with its dependencies
 * @todo implement log printing from container host, maybe use reactor to non-blocking interaction
 * */
@Log
class KomposerRunner {

    private DefaultDockerClient dockerClient
    private KomposerBuilder komposerBuilder

    def KomposerRunner() {
        this.dockerClient = new DefaultDockerClient(DefaultDockerClient.fromEnv())
        this.komposerBuilder = new KomposerBuilder(dockerClient)
    }

    def KomposerRunner(DockerClient client) {
        this.dockerClient = client
        this.komposerBuilder = new KomposerBuilder(dockerClient)
    }

    def KomposerRunner(String dockerCfgFile) {

        def props = new Properties()
        new File(dockerCfgFile).withInputStream {
            stream -> props.load(stream)
        }

        def host = props.host
        def certPath = props.'cert.path'
        def certificates
        if (certPath) {
            certificates = DockerCertificates.builder().dockerCertPath(Paths.get(certPath)).build()
        }

        log.info("Connecting to [${host}] using certificates from [${certPath}]")
        this.dockerClient = DefaultDockerClient.builder().uri(host).dockerCertificates(certificates).build()
        log.info(this.dockerClient.info().toString())

        this.komposerBuilder = new KomposerBuilder(dockerClient, props.'hub.user', props.'hub.pass', props.'hub.mail')
    }

    def up(String composeFile, pull = true) {
        if(!composeFile) {
            composeFile = 'docker-compose.yml'
        }

        def file = new File(composeFile)
        if (file.exists()) {

            log.info("Starting services on ${composeFile}")

            def configs = this.komposerBuilder.build(file, pull)

            def result = [:]
            configs.each { config ->

                def serviceName = config.key
                def containerName = config.value.name
                def containerConfig = config.value.container
                def hostConfig = config.value.host

                log.info("Starting service ${serviceName}")

                log.info("[$containerName] Creating container...")
                def creation = this.dockerClient.createContainer(containerConfig, containerName)

                log.info("[$containerName] Starting container...")
                dockerClient.startContainer(creation.id, hostConfig)

                log.info("[$containerName] Gathering container info...")
                def info = this.dockerClient.inspectContainer(creation.id)

                result[serviceName] = [containerId: creation.id, containerName: containerName, containerInfo: info] //containerLog: containerLog
            }

            return result
        } else {
            throw new FileNotFoundException("File [${file.absolutePath}] does not exists!")
        }
    }

    def down(services) {
        services.each { service ->
            def serviceName = service.key
            def containerName = service.value.containerName
            def containerId = service.value.containerId

            log.info("Stopping service ${serviceName} [${containerId} - ${containerName}]")
            try {
                dockerClient.killContainer(containerId)
            } catch (Exception e) {
                log.throwing('KomposerRunner', 'down', e)
            }
        }
    }

    def rm(services) {
        services.each { service ->
            def serviceName = service.key
            def containerName = service.value.containerName
            def containerId = service.value.containerId
            def removeVolumes = false

            if (service.value.removeVolumes) {
                removeVolumes = service.value.removeVolumes
            }

            log.info("Removing container [${containerId} - ${containerName}] from service ${serviceName}")

            try {
                dockerClient.removeContainer(containerId, removeVolumes)
            } catch (Exception e) {
                log.throwing('KomposerRunne', 'rm', e)
            }
        }
    }
}

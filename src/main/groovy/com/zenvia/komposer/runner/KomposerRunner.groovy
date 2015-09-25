package com.zenvia.komposer.runner

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerCertificates
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.LogStream
import com.zenvia.komposer.builder.KomposerBuilder
import com.zenvia.komposer.model.Komposition
import groovy.util.logging.Slf4j

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * @author Tiago Oliveira
 * @todo make possible to run one single container with its dependencies
 * @todo implement log printing from container host, maybe use reactor to non-blocking interaction
 * */
@Slf4j
class KomposerRunner {

    // create a method to allow exec on a container

    private DefaultDockerClient dockerClient
    private DefaultDockerClient originalDockerClient
    private final KomposerBuilder komposerBuilder
    private static final SECONDS_TO_KILL = 10
    private host
    private final privateNetwork
    private networkSetup

    def KomposerRunner() {
        this.dockerClient = new DefaultDockerClient(DefaultDockerClient.fromEnv())
        this.komposerBuilder = new KomposerBuilder(dockerClient)
    }

    def KomposerRunner(DockerClient client) {
        this.dockerClient = client
        this.komposerBuilder = new KomposerBuilder(dockerClient)
    }

    def KomposerRunner(String dockerCfgFile, privateNetwork = false) {

        def props = new Properties()
        new File(dockerCfgFile).withInputStream {
            stream -> props.load(stream)
        }

        host = props.host
        def certPath = props.'cert.path'
        def certificates
        if (certPath) {
            certificates = DockerCertificates.builder().dockerCertPath(Paths.get(certPath)).build()
        }

        log.info("Connecting to [${host}] using certificates from [${certPath}]")
        this.dockerClient = DefaultDockerClient.builder().apiVersion('v1.17').uri(host).dockerCertificates(certificates).build()

        if (privateNetwork) {
            this.privateNetwork = privateNetwork
            this.setupPrivateNetwork()
        }

        log.info(this.dockerClient.info().toString())

        this.komposerBuilder = new KomposerBuilder(dockerClient, props.'hub.user', props.'hub.pass', props.'hub.mail')
    }

    def setupPrivateNetwork() {
        this.networkSetup = new KomposerNetworkSetup()
        try {
            networkSetup.start(this.dockerClient)
            def host = networkSetup.getHost(this.dockerClient)

            this.originalDockerClient = this.dockerClient
            if (host) {
                host = 'http://' + host
                this.dockerClient = DefaultDockerClient.builder().apiVersion('v1.17').uri(host).build()
            }
        } catch (Exception e) {
            log.error("Impossible to start private network!", e)
        }
    }

    def String privateNetworkStatus() {
        return this.networkSetup.status(this.originalDockerClient)
    }

    def listAllContainers() {
        return this.dockerClient.listContainers(DockerClient.ListContainersParam.allContainers())
    }

    def up(String composeFile, Boolean pull = true, Boolean forcePull = false) {
        composeFile ?: 'docker-compose.yml'

        def file = new File(composeFile)
        if (file.exists()) {

            log.info("Starting services on ${composeFile}")

            def configs = this.komposerBuilder.build(file, pull, forcePull)

            def result = [:]
            configs.each { config ->

                def serviceName = config.key
                def containerName = config.value.name
                def containerConfig = config.value.container
                containerConfig.hostConfig = config.value.host

                log.info("Starting service ${serviceName}")

                log.info("[$containerName] Creating container...")
                def creation = this.dockerClient.createContainer(containerConfig, containerName)

                log.info("[$containerName] Starting container...")
                this.dockerClient.startContainer(creation.id)

                log.info("[$containerName] Gathering container info...")
                def info = this.dockerClient.inspectContainer(creation.id)

                LogStream logs = this.dockerClient.logs(creation.id, DockerClient.LogsParameter.FOLLOW,
                        DockerClient.LogsParameter.STDOUT,
                        DockerClient.LogsParameter.STDERR,
                        DockerClient.LogsParameter.TIMESTAMPS)

                Thread.start {
                    try {
                        while (logs.hasNext()) {
                            log.info("$containerName: ${StandardCharsets.US_ASCII.decode(logs.next().content()).toString()}")
                        }
                    } catch (Exception e) {
                        log.warn("Impossible to start logging thread")
                    }
                }

                result[serviceName] = new Komposition(containerId: creation.id, containerName: containerName, containerInfo: info, runner: this)
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
                log.error("Error stopping docker service $serviceName", e)
            }
        }

        if (this.privateNetwork) {
            this.dockerClient = this.originalDockerClient
            this.networkSetup.stop(this.dockerClient)
        }
    }

    def rm(services) {
        services.each { service ->
            def serviceName = service.key
            def containerName = service.value.containerName
            def containerId = service.value.containerId
            def removeVolumes = service.value.removeVolumes

            log.info("Removing container [${containerId} - ${containerName}] from service ${serviceName}")

            try {
                dockerClient.removeContainer(containerId, removeVolumes)
            } catch (Exception e) {
                log.error("Error removing container $containerName", e)
            }
        }
    }

    def stop(containerID) {
        this.dockerClient.stopContainer(containerID, SECONDS_TO_KILL)
        Thread.sleep(SECONDS_TO_KILL * 1000 + 2000)
        return this.dockerClient.inspectContainer(containerID)
    }

    def start(containerID) {
        this.dockerClient.startContainer(containerID)
        Thread.sleep(SECONDS_TO_KILL * 1000)
        return this.dockerClient.inspectContainer(containerID)
    }

    def exec(containerID, List command) {
        def executor = this.dockerClient.execCreate(containerID, command as String[], DockerClient.ExecParameter.STDERR, DockerClient.ExecParameter.STDOUT)
        def logs = this.dockerClient.execStart(executor)

        def result = ''
        while (logs.hasNext()) {
            result += StandardCharsets.US_ASCII.decode(logs.next().content()).toString()
        }

        return result
    }

    def finish() {
        this.dockerClient.close()
        this.dockerClient = null
    }

    def URI getHostUri() {
        return new URI(this.host)
    }
}

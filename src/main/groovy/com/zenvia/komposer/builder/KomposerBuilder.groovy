package com.zenvia.komposer.builder

import com.zenvia.komposer.model.docker.ContainerConfig
import com.zenvia.komposer.model.docker.HostConfig
import com.zenvia.komposer.model.docker.PortBinding
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientException
import groovy.util.logging.Log
import org.yaml.snakeyaml.Yaml

import java.nio.file.Paths

/**
 * @author Tiago Oliveira
 * */
@Log
class KomposerBuilder {

    private File composeFile
    private DockerClient client
    private hubLogin

    def KomposerBuilder(DockerClient client, String hubUser, String hubPass, String hubMail) {
        this.composeFile = composeFile
        this.client = client
        this.hubLogin = [user: hubUser, pass: hubPass, mail: hubMail]
    }

    def KomposerBuilder(DockerClient client) {
        this.composeFile = composeFile
        this.client = client
    }

    def build(File composeFile, pull = true) {
        log.info("Building containers from ${composeFile.absolutePath}")

        def prefix = Paths.get(composeFile.absolutePath).parent.fileName
        def instanceId = new Random().nextInt(1000)
        def namePattern = "komposer_${prefix}_%s_${instanceId}"

        def compose = new Yaml().load(composeFile.text)
        def result = [:]

        compose.reverseEach {
            String service = it.key

            log.info "Processing service: $service"

            def containerConfig = this.createContainerConfig(it.value, service, namePattern, pull)
            def hostConfig = this.createHostConfig(it.value, namePattern)
            def containerName = sprintf(namePattern, [service])

            result[service] = [name: containerName, container: containerConfig, host: hostConfig]
        }

        return result
    }

    def ContainerConfig createContainerConfig(service, serviceName, namePattern, pull = true) {
        log.info("Creating container config for [${serviceName}], pullImage = ${pull}")

        ContainerConfig builder = new ContainerConfig()

        def imageName = service.image
        if (imageName && pull) {
            this.pullImage((String)imageName)
        } else if (service.build) {
            throw new DockerClientException("Not implemented")
            //imageName = this.buildImage(service.build, serviceName, namePattern)
        }

        def exposed = []
        service.expose?.each {
            def port = it
            if (!port.contains("/")) {
                port = "$port/tcp"
            }
            exposed += [port]
        }

        service.ports?.each { String iPort ->
            def port = iPort.split(':')[0]
            if (!port.contains("/")) {
                port = "$port/tcp"
            }
            exposed += [port]
        }

        def envs = []
        service.environment?.each {
            envs += [it]
        }

        def volumes = []
        service.volumes?.each {
            volumes += [it]
        }

        builder.image = imageName

        if (service.cmd) {
            builder.cmd = Arrays.asList((String)service.cmd)
        }

        builder.exposedPorts = exposed.toSet()
        builder.env = envs
        builder.volumes = volumes.toSet()

        return builder
    }

    def createHostConfig(service, namePattern) {
        log.info("Creating host config...")
        def builder = new HostConfig()

        def ports = [:]
        service.ports?.each { String mapping ->
            log.finest("Processing $mapping")

            def externalPort = ''
            def internalPort = ''
            def host = '0.0.0.0'

            def separatorCount = (mapping =~ ':').count
            if (separatorCount > 0) {
                switch (separatorCount) {
                    case 1:
                        externalPort = mapping.split(':')[0]
                        internalPort = mapping.split(':')[1]
                        break
                    case 2:
                        host = mapping.split(':')[0]
                        externalPort = mapping.split(':')[1]
                        internalPort = mapping.split(':')[2]
                        break
                }
            } else {
                internalPort = mapping
                externalPort = mapping
            }

            if (internalPort && !internalPort.contains('/')) {
                internalPort += '/tcp'
            }

            if (externalPort && !externalPort.contains('/')) {
                externalPort += '/tcp'
            }

            log.fine("Port mapping $internalPort -> $externalPort:$host")
            ports[internalPort] = [
                    new PortBinding(hostIp: host, hostPort: externalPort)
            ]
        }

        def links = []
        service.links?.each { String link ->
            log.finest("Processing $link")

            def linkAlias
            def linkContainer

            if (link.contains(':')) {
                linkAlias = link.split(':')[1]
                linkContainer = link.split(':')[0]
            } else {
                linkAlias = link
                linkContainer = linkAlias
            }

            linkContainer = sprintf(namePattern, [linkContainer])
            def linkUrl = linkContainer + ':' + linkAlias

            log.fine("Creating link $linkUrl ")
            links += [linkUrl]
        }

        builder.portBindings = ports
        builder.links = links
        if (service.net) {
            builder.networkMode = service.net
        }

        return builder.asMap()
    }

    /*def pullImage(String image) {
        if (!image.contains(':')) {
            image += ':latest'
        }

        def progress = new ProgressHandler() {
            @Override
            void progress(ProgressMessage message) throws DockerException {

                if (message.error()) {
                    throw new DockerException(message.error())
                }

                if (message.progressDetail() != null) {
                    final String id = message.id()

                    String progress = message.progress()
                    if (!progress) {
                        progress = ""
                    }

                    log.fine(sprintf("%s: %s %s%n", [id, message.status(), progress]))
                } else {

                    String value = message.stream()
                    if (value) {
                        value = value.trim()
                    } else {
                        value = message.status()
                    }

                    if (!value) {
                        value = message.toString()
                    }

                    log.info(value)
                }
            }
        }

        try {
            if (this.hubLogin && this.hubLogin.user) {
                log.info("Pulling image [${image}] using authentication...")
                AuthConfig auth = AuthConfig.builder().username(this.hubLogin.user).password(this.hubLogin.pass).email(this.hubLogin.mail).build()
                this.client.pull(image, auth)
            } else {
                log.info("Pulling image [${image}] without auth...")
                this.client.pull(image)
            }
        }  catch (Exception e) {
            def message = "Impossible to pull the image from repository, please do it manually"
            log.severe(message)
            throw new DockerException(message, e)
        }
    }*/

    def pullImage (String image) {
        if (!image.contains(':')) {
            image += ':latest'
        }

        try {
            if (this.hubLogin && this.hubLogin.user) {
                log.info("Authenticating on DockerHub with credentials: ${this.hubLogin}")
                this.authenticateOnDockerHub((String)this.hubLogin.user, (String)this.hubLogin.pass, (String)this.hubLogin.mail)
            }
            this.client.pull(image)
        }
        catch (Exception e) {
            def message = "Impossible to pull the image from repository, please do it manually"
            log.severe(message)
            throw new DockerClientException(message, e)
        }
    }

    def authenticateOnDockerHub (String username, String password, String email) {
        def authConfig = [
                            "username"     : username,
                            "password"     : password,
                            "email"        : email,
                            "serveraddress": "https://index.docker.io/v1/"
                         ]
        this.client.auth(authConfig)
    }

    /*
        TODO: Check how implement CompressedDirectory
    */
    /*def buildImage(String path, serviceName, namePatterm) {
        log.info("Building image on [${path}]")
        def imageName = sprintf(namePatterm, [serviceName])
        //def ph = new AnsiProgressHandler()
        InputStream is = new FileInputStream(Paths.get(path).toAbsolutePath().toString())
        this.client.build(is)
        return imageName
    }*/
}

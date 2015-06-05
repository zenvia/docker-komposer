package com.zenvia.komposer.runner

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.ContainerInfo
import spock.lang.Specification

/**
 * @author Tiago de Oliveira
 * */
class KomposerRunnerSpec extends Specification {

    DefaultDockerClient dockerClient = Mock(constructorArgs: [DefaultDockerClient.fromEnv()])
    def runner = new KomposerRunner(dockerClient)
    def services = ['sender': [containerId: '9998877', containerName: 'komposer_resources_sender_']]

    def "Up"() {
        given:
            def file = 'src/test/resources/docker-compose.yml'
            def creation = new ContainerCreation()
            creation.id = '9998877'
            def info = new ContainerInfo()
        when:
            dockerClient.createContainer(_, _) >> creation
            dockerClient.inspectContainer(creation.id) >> info
            def result = runner.up(file)
        then:
            result
            result.sender.containerId == services.sender.containerId
            result.sender.containerName.contains(services.sender.containerName)
            result.sender.containerInfo == info
    }

    def "Down"() {
        when:
            runner.down(services)
        then:
            dockerClient.killContainer('9998877')
    }

    def "Rm"() {
        when:
            runner.rm(services)
        then:
            dockerClient.removeContainer('9998877')
    }
}

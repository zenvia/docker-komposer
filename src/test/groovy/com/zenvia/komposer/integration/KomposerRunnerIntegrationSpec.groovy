package com.zenvia.komposer.integration

import com.zenvia.komposer.runner.KomposerRunner
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Tiago de Oliveira
 * */
@Ignore
@Slf4j
class KomposerRunnerIntegrationSpec extends Specification {

    def runner, containers

    def "start a container with a private network and leave the room clean when getting out"() {
        given: 'a compose file with two containers without any link between'
            def privateNetwork = true
            def dockerConfigFile = 'src/test/resources/docker.properties'
            def dockerComposeFile = 'src/test/resources/docker-compose-test.yml'
        when: 'I create a komposerRunner with a private network'
            runner = new KomposerRunner(dockerConfigFile, privateNetwork)
        and: 'I start the runner'
            containers = runner.up(dockerComposeFile, true)
        then: 'I expect the containers to be created'
            containers
        and: 'the docker client to be running with the new proxy server'
            runner.dockerClient.uri.authority.split(':').last() == ('12375')
        and: 'a private network to be bonding the containers together'
            runner.privateNetworkStatus().contains('weave proxy is running')
        when: 'I try to connect from one container to another using the private network'
            def execLogs = containers.machine01.exec(['ping', '-c', '4', 'machine02'])
        then: 'the containers should resolving the hostname and connecting to each other'
            execLogs.contains('PING machine02.weave.local')
            execLogs.contains('machine02.weave.local ping statistics')
            execLogs.contains('4 packets transmitted, 4 received, 0% packet loss')
        when: 'I stop the composition'
            runner.down(containers)
            runner.rm(containers)
        then: 'I expect the containers to be killed'
            !runner.listAllContainers()
        and: 'the private network to be destroyed'
            runner.privateNetworkStatus().contains('weave container is not present. Have you launched it?')
        and: 'the docker client to be using the real host'
            runner.dockerClient.uri.authority.split(':').last() != '12375'
    }

    def cleanup() {
        try {
            if (runner) {
                runner.down(containers)
                runner.rm(containers)
            }
        } catch (Exception e) {
            log.error("Error on clean up ", e)
        }
    }

}

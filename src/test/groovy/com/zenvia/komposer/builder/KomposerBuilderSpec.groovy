package com.zenvia.komposer.builder

import com.spotify.docker.client.DockerClient
import groovy.util.logging.Log
import spock.lang.Specification

/**
 * @author Tiago de Oliveira
 * */
@Log
class KomposerBuilderSpec extends Specification {

    def client = Mock(DockerClient)
    def builder = new KomposerBuilder(client)

    def 'create container config with all parameters'() {
        given:
            def service = [image: 'mysql', expose: ['3000', '3001/tcp', '3002/udp'], environment: ['HEY=HELLO'], volumes: ['/var/lib/test'], cmd: 'echo test']
        when:
            def response = builder.createContainerConfig(service, 'test', 'komposer_%s_1')
        then:
            response
            response.image() == service.image
            response.cmd() == [service.cmd]
            response.exposedPorts().toArray().contains('3000/tcp')
            response.exposedPorts().toArray().contains('3002/udp')
            response.volumes().toArray().contains('/var/lib/test')
            response.env().toArray().contains('HEY=HELLO')
    }

    def 'create container config only with image name'() {
        given:
            def service = [image: 'mysql']
        when:
            def response = builder.createContainerConfig(service, 'test', 'komposer_%s_1')
        then:
            response
            response.image() == service.image
            !response.cmd()
            !response.exposedPorts()
            !response.volumes()
            !response.env()
    }

    def 'create container with build tag'() {
        given:
            def service = [build: '.', expose: ['3000', '3001/tcp', '3002/udp'], environment: ['HEY=HELLO'], volumes: ['/var/lib/test'], cmd: 'echo test']
        when:
            def response = builder.createContainerConfig(service, 'test', 'komposer_%s_1')
        then:
            response
            response.image() == 'komposer_test_1'
            response.cmd() == [service.cmd]
            response.exposedPorts().toArray().contains('3000/tcp')
            response.exposedPorts().toArray().contains('3002/udp')
            response.volumes().toArray().contains('/var/lib/test')
            response.env().toArray().contains('HEY=HELLO')
    }


    def 'create host config with all params'() {
        given:
            def service = [ports: ['127.0.0.1:8081:8082', '8083:8084', '8085'], links: ['db:database', 'mongo'], net: 'host']
        when:
            def response = builder.createHostConfig(service, 'komposer_%s_1')
        then:
            response
            response.networkMode() == 'host'
            response.links().toArray().contains('komposer_db_1:database')
            response.links().toArray().contains('komposer_mongo_1:mongo')
            response.portBindings().get('8085')
            response.portBindings().get('8085')[0].hostPort() == '8085'
            response.portBindings().get('8085')[0].hostIp() == '0.0.0.0'
            response.portBindings().get('8082')[0].hostPort() == '8081'
            response.portBindings().get('8082')[0].hostIp() == '127.0.0.1'
            response.portBindings().get('8084')[0].hostPort() == '8083'
            response.portBindings().get('8084')[0].hostIp() == '0.0.0.0'
    }

    def 'create host config without params'() {
        given:
            def service = []
        when:
            def response = builder.createHostConfig(service, 'komposer_%s_1')
        then:
            response
    }

    def 'build from compose file'() {
        given:
            def file = new File('src/test/resources/docker-compose.yml')
        when:
            def response = builder.build(file)
        then:
            response
            response.provider
            response.provider.container
            response.provider.host
            response.provider.container.image == 'tiagodeoliveira/micro-arch-provider'
            response.provider.container.env.toArray().contains('DB_PASS=test')
            response.provider.host.portBindings().get('40084')
            response.provider.host.portBindings().get('40084')[0].hostPort == '40084'
            response.provider.host.portBindings().get('40084')[0].hostIp == '0.0.0.0'
            response.provider.host.links[0].contains('komposer_resources_receiver_')
            response.sender.container.exposedPorts.toArray().contains('40081/tcp')
    }

}

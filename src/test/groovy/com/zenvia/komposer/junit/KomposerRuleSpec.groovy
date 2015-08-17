package com.zenvia.komposer.junit

import groovy.util.logging.Log
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Tiago de Oliveira
 * @since 6/10/15
 */
@Log
@Ignore
class KomposerRuleSpec extends Specification {

    static KomposerRule rule

    def setupSpec() {
        def composeFile = 'src/test/resources/docker-compose-test.yml'
        def cfgFile = 'src/test/resources/docker.properties'
        rule = new KomposerRule(composeFile, cfgFile, false)
    }

    def "getContainers"() {
        when:
            rule.before()
        then:
            rule.getContainers()
            rule.getContainers().get('node')
            rule.getContainers().get('node').containerId
            rule.getContainers().get('lifecyclemanager').containerInfo.networkSettings().ports()
    }

    def "stopContainers"() {
        when:
            rule.before()
        then:
            rule.getContainers().get("redis").containerInfo.state().running()
            rule.stop("redis")
            !rule.getContainers().get("redis").containerInfo.state().running()
    }

    def "startContainers"() {
        when:
        rule.before()
        then:
        rule.getContainers().get("redis").containerInfo.state().running()
        rule.stop("redis")
        !rule.getContainers().get("redis").containerInfo.state().running()
        rule.start("redis")
        rule.getContainers().get("redis").containerInfo.state().running()

    }

    def "getHostURI"() {
        setup:
            rule.before()
        when:
            URI hostUri = rule.getHostURI()
        then:
            hostUri.getHost() == "teste"
            hostUri.getPort() == 5656
            hostUri.getScheme() == "http"
    }

    def cleanupSpec() {
        rule.after()
    }
}

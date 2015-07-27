package com.zenvia.komposer.model.docker

import groovy.json.JsonSlurper
import spock.lang.Specification
import com.google.common.collect.Maps

/**
 * Created by klein on 17/07/15.
 */
class ContainerConfigTest extends Specification {
    def "AsMap"() {
        given:
            ContainerConfig config = new ContainerConfig()
            config.Hostname = ''
            config.Domainname = ''
            config.User = ''
            config.Memory = '0'
            config.MemorySwap = '0'
            config.CpuShares = '512'
            config.Cpuset = '0,1'
            config.AttachStdin = false
            config.AttachStdout = true
            config.AttachStderr = true
            config.Tty = false
            config.OpenStdin = false
            config.StdinOnce = false
            config.Env = null
            config.Cmd = ['date']
            config.Entrypoint = ""
            config.Image = 'ubuntu'
            config.Volumes = ['/tmp':[:]]
            config.WorkingDir = ""
            config.NetworkDisabled = false
            config.MacAddress = '12:34:56:78:9a:bc'
            config.ExposedPorts = ["22/tcp":[:]]
            config.HostConfig = [:]
            config.HostConfig["Binds"] = ["/tmp:/tmp"]
            config.HostConfig["CapAdd"] = ["NET_ADMIN"]
            config.HostConfig["CapDrop"] = ["MKNOD"]
            config.HostConfig["Devices"] = []
            config.HostConfig["Dns"] = ["8.8.8.8"]
            config.HostConfig["DnsSearch"] = [""]
            config.HostConfig["ExtraHosts"] = null
            config.HostConfig["Links"] = ["redis3:redis"]
            config.HostConfig["LxcConf"] = ["lxc.utsname":"docker"]
            config.HostConfig["NetworkMode"] = "bridge"
            config.HostConfig["PortBindings"] = [ "22/tcp": [[ "HostPort": "11022" ]]]
            config.HostConfig["Privileged"] = false
            config.HostConfig["PublishAllPorts"] = false
            config.HostConfig["ReadonlyRootfs"] = false
            config.HostConfig["RestartPolicy"] = ["MaximumRetryCount": 0, "Name": ""]
            config.HostConfig["SecurityOpt"] = [""]
            config.HostConfig["VolumesFrom"] = ["parent", "other:ro"]
        when:
            String map1AsStr = Maps.newHashMap(new JsonSlurper().parse(new File('src/test/resources/docker_container_config_v1.17.json'))).toString()
            String map2AsStr = Maps.newHashMap(config.asMap().sort()).toString()
        then:
            map1AsStr.equals(map2AsStr)
    }
}

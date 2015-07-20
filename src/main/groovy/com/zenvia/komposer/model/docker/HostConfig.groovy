package com.zenvia.komposer.model.docker

/**
 * Created by klein on 17/07/15.
 */
class HostConfig {

    def List<String> binds;
    def String containerIDFile;
    def List<LxcConfParameter> lxcConf;
    def Boolean privileged;
    def Map<String, List<PortBinding>> portBindings;
    def List<String> links;
    def Boolean publishAllPorts;
    def List<String> dns;
    def List<String> dnsSearch;
    def List<String> volumesFrom;
    def String networkMode;
    def List<String> securityOpt;
    def Long memory;
    def Long memorySwap;
    def Long cpuShares;
    def String cpusetCpus;
    def String cgroupParent;

    def asMap() {
        def values = [:]
        this.properties.each {
            println(it)
            if (it.key != 'class') {
                values[it.key] = it.value
            }
        }
        return values
    }
}

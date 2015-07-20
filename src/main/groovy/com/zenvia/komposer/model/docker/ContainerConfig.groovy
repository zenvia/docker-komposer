package com.zenvia.komposer.model.docker

/**
 * Created by klein on 17/07/15.
 */
class ContainerConfig {
    def Boolean AttachStdin;
    def Boolean AttachStdout;
    def Boolean AttachStderr;
    def String CpuShares;
    def String Cpuset;
    def List<String> Cmd;
    def String Domainname;
    def List<String> Env;
    def Map ExposedPorts;
    def String Entrypoint;
    def String Hostname;
    def Map HostConfig;
    def String Image;
    def String Memory;
    def String MemorySwap;
    def String MacAddress;
    def Boolean NetworkDisabled;
    def Boolean OpenStdin;
    def Boolean StdinOnce;
    def Boolean Tty;
    def String User;
    def Map Volumes;
    def String WorkingDir;
    //def List<String> OnBuild;
    //def Map<String, String> Labels;

    def asMap() {
        def values = [:]
        this.properties.each {
            println(it)
            if (it.key != 'class') {
                values[it.key.toString().capitalize()] = it.value.toString()
            }
        }
        return values
    }
}

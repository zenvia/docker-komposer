package com.zenvia.komposer.model

import com.spotify.docker.client.messages.ContainerInfo
import com.zenvia.komposer.runner.KomposerRunner

/**
 * @author Tiago de Oliveira
 * @since 6/11/15
 */
class Komposition {
    String containerId
    String containerName
    ContainerInfo containerInfo
    Boolean removeVolumes = true
    KomposerRunner runner

    public Komposition(args) {
        this.containerId = args.containerId
        this.containerName = args.containerName
        this.containerInfo = args.containerInfo
        this.runner = args.runner
    }

    public String getContainerId() {
        return this.containerId
    }

    public String getContainerName() {
        return this.containerName
    }

    public ContainerInfo getContainerInfo() {
        return this.containerInfo
    }

    public Boolean getRemoveVolumes() {
        return this.removeVolumes
    }

    public String exec(List command) {
        this.runner.exec(this.containerId, command)
    }
}

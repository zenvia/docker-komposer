package com.zenvia.komposer.model

import com.spotify.docker.client.messages.ContainerInfo

/**
 * @author Tiago de Oliveira
 * @since 6/11/15
 */
class Komposition {
    private final String containerId
    private final String containerName
    private final ContainerInfo containerInfo
    private final Boolean removeVolumes = false

    public Komposition(args) {
        this.containerId = args.containerId
        this.containerName = args.containerName
        this.containerInfo = args.containerInfo
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
}

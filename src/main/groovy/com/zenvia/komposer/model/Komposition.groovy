package com.zenvia.komposer.model

import com.zenvia.komposer.model.docker.ContainerInfo

/**
 * @author Tiago de Oliveira
 * @since 6/11/15
 */
class Komposition {
    private String containerId
    private String containerName
    private ContainerInfo containerInfo
    private Boolean removeVolumes = false

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

package com.zenvia.komposer.model.docker

/**
 * Created by klein on 17/07/15.
 */
class ContainerState {
    def Boolean running;
    def Boolean paused;
    def Boolean restarting;
    def Integer pid;
    def Integer exitCode;
    def Date startedAt;
    def Date finishedAt;
    def String error;
    def Boolean oomKilled;
}

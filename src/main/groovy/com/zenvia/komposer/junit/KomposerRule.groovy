package com.zenvia.komposer.junit

import com.zenvia.komposer.model.Komposition
import com.zenvia.komposer.runner.KomposerRunner
import org.junit.rules.ExternalResource

/**
 * @author Tiago de Oliveira
 * */
class KomposerRule extends ExternalResource {

    private KomposerRunner runner
    private String composeFile
    private runningServices
    private pull = true

    def KomposerRule(String compose, String dockerCfg, Boolean pull = true) {
        this.runner = new KomposerRunner(dockerCfg)
        this.composeFile = compose
        this.pull = pull
    }

    def KomposerRule(String compose, Boolean pull = true) {
        this.runner = new KomposerRunner()
        this.composeFile = compose
        this.pull = pull
    }

    def KomposerRule(String compose, KomposerRunner runner) {
        this.runner = runner
        this.composeFile = composeFile
    }

    @Override
    void before() throws Throwable {
        this.runningServices = this.runner.up(this.composeFile, pull)
    }

    @Override
    void after() {
        this.runner.down(this.runningServices)
        this.runner.rm(this.runningServices)
        this.runner.finish()
    }

    def Map<String, Komposition> getContainers() {
        return this.runningServices
    }
}

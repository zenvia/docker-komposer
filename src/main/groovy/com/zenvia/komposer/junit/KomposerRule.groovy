package com.zenvia.komposer.junit

import com.zenvia.komposer.runner.KomposerRunner
import org.junit.rules.ExternalResource

/**
 * @author Tiago de Oliveira
 * */
class KomposerRule extends ExternalResource {

    private KomposerRunner runner
    private String composeFile
    private runningServices

    def KomposerRule(String compose) {
        if (!compose) {
            compose = 'docke-compose-test.yml'
        }

        this.runner = new KomposerRunner()
        this.composeFile = compose
    }

    def KomposerRule(String compose, KomposerRunner runner) {
        this.runner = runner
        this.composeFile = composeFile
    }

    void before() throws Throwable {
        this.runningServices = this.runner.up(this.composeFile)
    }

    void after() {
        this.runner.down(this.runningServices)
        this.runner.rm(this.runningServices)
    }
}

package ir.rosheno.launcher.domain.repository

import ir.rosheno.launcher.domain.model.LaunchableApp

interface AppRepository { fun load(): List<LaunchableApp> }

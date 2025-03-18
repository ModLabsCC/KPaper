package cc.modlabs.kpaper.visuals.effect

import org.bukkit.Location
import org.bukkit.entity.Entity

interface ParticleEffect {

    fun play(locations: Set<Location>, entities: Set<Entity>)

    fun play(location: Location, entities: Set<Entity>) =
        play(setOf(location), entities)

    fun play(entity: Entity, locations: Set<Location>) =
        play(locations, setOf(entity))

    fun play(location: Location, entity: Entity) =
        play(setOf(location), setOf(entity))

    fun play(vararg entities: Entity?)

    fun play(vararg locations: Location?)

    fun play()
}
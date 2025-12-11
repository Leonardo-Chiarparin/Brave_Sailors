package com.example.brave_sailors.ui.utils

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class RadarBlip(val angle: Float, val distance: Float)

fun generateBlips(count: Int, minDistance: Float, minRadius: Float, maxRadius: Float, outerRadius: Float): List<RadarBlip> {
    val blips = mutableListOf<RadarBlip>()

    var attempts = 0
    val maxAttempts = 100

    while((blips.size < count) && (attempts < maxAttempts)) {
        val candidateAngle = Random.nextFloat() * 360f
        val candidateDistance = ( Random.nextFloat() * ( maxRadius - minRadius ) ) + minRadius

        val candidateRadians = Math.toRadians(candidateAngle.toDouble())

        val candidateX = ( candidateDistance * outerRadius ) * cos(candidateRadians)
        val candidateY = ( candidateDistance * outerRadius ) * sin(candidateRadians)

        var collides = false

        for(blip in blips) {
            val blipRadians = Math.toRadians(blip.angle.toDouble())

            val blipX = ( blip.distance * outerRadius ) * cos(blipRadians)
            val blipY = ( blip.distance * outerRadius ) * sin(blipRadians)

            val distance = sqrt((candidateX - blipX).pow(2) + (candidateY - blipY).pow(2))

            if(distance < minDistance) {
                collides = true
                break
            }
        }

        if(!collides)
            blips.add(RadarBlip(candidateAngle, candidateDistance))

        attempts++
    }

    return blips
}


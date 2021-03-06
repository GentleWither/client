package me.zeroeightsix.kami.module.modules.combat

import me.zeroeightsix.kami.event.SafeClientEvent
import me.zeroeightsix.kami.event.events.PacketEvent
import me.zeroeightsix.kami.module.Category
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.module.modules.movement.Strafe
import me.zeroeightsix.kami.util.combat.SurroundUtils
import me.zeroeightsix.kami.util.math.RotationUtils
import me.zeroeightsix.kami.util.math.VectorUtils
import me.zeroeightsix.kami.util.math.VectorUtils.distanceTo
import me.zeroeightsix.kami.util.math.VectorUtils.toBlockPos
import me.zeroeightsix.kami.util.math.VectorUtils.toVec3dCenter
import me.zeroeightsix.kami.util.threads.safeListener
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.kamiblue.commons.extension.ceilToInt
import org.kamiblue.commons.extension.toRadian
import org.kamiblue.event.listener.listener
import kotlin.math.*

internal object HoleSnap : Module(
    name = "HoleSnap",
    description = "Move you into the hole nearby",
    category = Category.COMBAT
) {
    private val disableStrafe by setting("DisableStrafe", true)
    private val range by setting("Range", 2.5f, 0.5f..4.0f, 0.25f)

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) disable()
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (SurroundUtils.checkHole(player) != SurroundUtils.HoleType.NONE) {
                disable()
                return@safeListener
            }
            findHole()?.toVec3dCenter()?.let {
                if (disableStrafe) Strafe.disable()
                if (player.onGround) {
                    val yawRad = RotationUtils.getRotationTo(player.positionVector, it).x.toDouble().toRadian()
                    val speed = min(0.25, player.positionVector.distanceTo(it) / 4.0)
                    player.motionX = -sin(yawRad) * speed
                    player.motionZ = cos(yawRad) * speed
                }
            }
        }
    }

    private fun SafeClientEvent.findHole(): BlockPos? {
        var closestHole = Pair(69.69, BlockPos.ORIGIN)
        val playerPos = player.positionVector.toBlockPos()
        val ceilRange = (range).ceilToInt()
        val posList = VectorUtils.getBlockPositionsInArea(playerPos.add(ceilRange, -1, ceilRange), playerPos.add(-ceilRange, -1, -ceilRange))
        for (posXZ in posList) {
            val dist = player.distanceTo(posXZ)
            if (dist > range || dist > closestHole.first) continue
            for (posY in 0..5) {
                val pos = posXZ.add(0, -posY, 0)
                if (!world.isAirBlock(pos.up())) break
                if (SurroundUtils.checkHole(pos) == SurroundUtils.HoleType.NONE) continue
                closestHole = dist to pos
            }
        }
        return if (closestHole.second != BlockPos.ORIGIN) closestHole.second else null
    }
}
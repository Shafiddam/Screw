package com.fidapp.screw

import com.fidapp.screw.game.GameEngine
import com.fidapp.screw.game.HandcraftedLevels
import com.fidapp.screw.game.LevelGenerator
import com.fidapp.screw.game.Levels
import com.fidapp.screw.game.contains
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Каждый уровень (ручные 1..15 и генератор до 240) обязан проходиться
 * жадным решателем движка: ящики-заказы + склад из 5 кружков не должны
 * приводить к тупику при разумной игре.
 */
class LevelSolvabilityTest {

    @Test
    fun allLevelsSolvableByGreedy() {
        val unsolvable = ArrayList<Int>()
        for (number in 1..240) {
            val engine = GameEngine(Levels.forNumber(number))
            if (!engine.greedySolve()) unsolvable += number
        }
        assertTrue("unsolvable levels: $unsolvable", unsolvable.isEmpty())
    }

    @Test
    fun generatorIsDeterministic() {
        val a = LevelGenerator().generate(37)
        val b = LevelGenerator().generate(37)
        assertTrue(a.screws == b.screws && a.pieces == b.pieces)
    }

    @Test
    fun screwsStayInsideTheirPiece() {
        HandcraftedLevels.all().forEach { level ->
            level.screws.forEach { screw ->
                val piece = level.pieces.first { p -> p.id == screw.pieceId }
                assertTrue(
                    "уровень ${level.number}: шуруп ${screw.id} вне детали ${piece.id}",
                    piece.contains(screw.x, screw.y),
                )
            }
        }
    }
}

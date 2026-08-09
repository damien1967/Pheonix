package phoenix.piece

data class PieceSource(
    private val slots: List<PieceSlot>
) {

    val filledSlotCount: Int
        get() = slots.count { it is PieceSlot.Filled }

    fun slotAt(index: Int): PieceSlot {
        requireInBounds(index)
        return slots[index]
    }

    fun withSlotFilled(index: Int, piece: GamePiece): PieceSource {
        requireInBounds(index)
        return withSlot(index, PieceSlot.Filled(piece))
    }

    fun withSlotEmptied(index: Int): PieceSource {
        requireInBounds(index)
        return withSlot(index, PieceSlot.Empty)
    }

    private fun withSlot(index: Int, newSlot: PieceSlot): PieceSource {
        val updatedSlots = slots.mapIndexed { slotIndex, slot ->
            if (slotIndex != index) slot else newSlot
        }
        return copy(slots = updatedSlots)
    }

    private fun requireInBounds(index: Int) {
        require(index in slots.indices) {
            "Slot index $index is out of bounds for a source with ${slots.size} slots"
        }
    }

    companion object {
        fun create(slotCount: Int): PieceSource {
            return PieceSource(slots = List(slotCount) { PieceSlot.Empty })
        }
    }
}

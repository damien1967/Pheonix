package phoenix.piece

data class PieceShape(
    val offsets: List<CellOffset>
) {

    fun rotated(): PieceShape {
        val rotatedOffsets = offsets.map { offset ->
            CellOffset(row = offset.column, column = -offset.row)
        }
        return copy(offsets = rotatedOffsets)
    }
}

package com.dbook.domain.seating

private val ROW_LETTERS = ('A'..'Z').toList()

/**
 * The seats of positions [from] until [until] on an aircraft: rows of as many seats as its layout has per row,
 * labelled "1A", "1B"... (the seat at position `i` is row `i / perRow + 1`, letter `i % perRow`). The one place seat
 * labels are made.
 */
fun generateSeats(
    bookableId: Long,
    aircraftType: String,
    from: Int,
    until: Int,
): List<Seat> {
    val perRow = seatLayoutFor(aircraftType).sum()
    return (from until until).map { index ->
        Seat(bookableId = bookableId, label = "${index / perRow + 1}${ROW_LETTERS[index % perRow]}")
    }
}

/** The position of a seat on the aircraft, from its label: "10A" comes after "9F", which sorting the text would not. */
fun seatPosition(
    label: String,
    aircraftType: String,
): Int {
    val perRow = seatLayoutFor(aircraftType).sum()
    val row = label.dropLast(1).toInt()
    return (row - 1) * perRow + ROW_LETTERS.indexOf(label.last())
}

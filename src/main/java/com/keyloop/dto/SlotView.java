package com.keyloop.dto;

/**
 * One candidate start time in a day view. {@code available} reflects resource
 * capacity (a free technician + bay for the full duration). {@code mineBooked} is
 * true when the queried vehicle already has a CONFIRMED appointment overlapping
 * this window — so the client can disable it rather than let the booking 409.
 */
public record SlotView(String start, boolean available, int technicians, int bays, boolean mineBooked) {
}

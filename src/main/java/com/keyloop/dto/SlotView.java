package com.keyloop.dto;

/** One candidate start time in a day view, with remaining resource counts. */
public record SlotView(String start, boolean available, int technicians, int bays) {
}

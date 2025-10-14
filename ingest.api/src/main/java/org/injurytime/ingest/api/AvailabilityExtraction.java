/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.injurytime.ingest.api;

import java.time.LocalDate;

public record AvailabilityExtraction(
        String playerName,
        String clubName,
        String availabilityType,
        String reasonSubtype,
        String status,
        LocalDate startDate,
        LocalDate expectedReturnDate,
        Integer expectedDurationDays,
        String headline,
        String snippet,
        String canonicalUrl,
        int confidence,
        String sourceSystem,
        String sourceUri
        ) {

}

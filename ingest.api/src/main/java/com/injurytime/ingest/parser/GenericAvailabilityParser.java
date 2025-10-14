/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.ingest.parser;

import org.injurytime.ingest.api.AvailabilityExtraction;
import org.injurytime.ingest.api.AvailabilityParser;
import org.injurytime.ingest.api.RawItem;

import java.util.List;
import java.util.regex.Pattern;

public class GenericAvailabilityParser implements AvailabilityParser {

    private static final Pattern INJURY = Pattern.compile("hamstring|acl|meniscus|ankle|groin|calf|shoulder|concussion|fracture|metatarsal|back|hip|knee|achilles", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUSP = Pattern.compile("red card|sent off|dismissed|ban(ned)?|suspension|accumulation|fifth yellow|second yellow", Pattern.CASE_INSENSITIVE);

    @Override
    public String id()
    {
        return "generic-news-parser";
    }

    @Override
    public List<AvailabilityExtraction> parse(RawItem item)
    {
        var text = (item.text() == null ? "" : item.text()) + "\n" + (item.subject() == null ? "" : item.subject());
        String avType = "unknown", subtype = null, status = "unknown";
        if (SUSP.matcher(text).find())
        {
            avType = "suspension";
            subtype = text.toLowerCase().contains("red card") || text.toLowerCase().contains("sent off") ? "red_card" : (text.toLowerCase().contains("yellow") ? "yellow_accumulation" : null);
            status = text.toLowerCase().matches(".*(ban|suspension|sidelined).*") ? "out" : "unknown";
        } else if (INJURY.matcher(text).find())
        {
            avType = "injury";
            status = text.toLowerCase().matches(".*(out|sidelined|ruled out|surgery).*") ? "out" : (text.toLowerCase().matches(".*(doubtful|late fitness test|to be assessed).*") ? "doubtful" : "unknown");
        }
        int confidence = !"unknown".equals(avType) ? 10 : 0;
// TODO: detect person name (use a simple heuristic or integrate an NLP lib if desired)
        String playerName = null;
        var ex = new AvailabilityExtraction(
                playerName, null, avType, subtype, status, null, null, null,
                item.subject(), item.text(), item.sourceUri(), confidence, item.sourceSystem(), item.sourceUri()
        );
        return List.of(ex);
    }
}

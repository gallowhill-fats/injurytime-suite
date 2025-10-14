/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.injurytime.ingest.api;

import java.util.List;

/**
 *
 * @author clayton
 */
public interface SourceConnector {

    String id();

    List<RawItem> fetchNew() throws Exception;
}

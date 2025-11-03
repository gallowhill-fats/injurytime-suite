/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.api;

 /** Score histogram bin (e.g., “1-0”, 14) — with a special label “other”. */
  record ScoreBin(String label, int count) {}

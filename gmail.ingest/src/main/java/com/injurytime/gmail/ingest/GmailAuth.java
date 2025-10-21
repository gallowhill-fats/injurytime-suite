/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.gmail.ingest;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

public final class GmailAuth {
  private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_READONLY);

  public static Gmail buildService() throws Exception {
    String home = System.getProperty("user.home");
    File creds = new File(home + File.separator + ".injurytime" + File.separator + "google" + File.separator + "client_secret.json");
    File tokensDir = new File(home + File.separator + ".injurytime" + File.separator + "google" + File.separator + "tokens");
    if (!creds.isFile()) throw new IllegalStateException("Missing client_secret.json at " + creds);

    NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
    try (var in = Files.newInputStream(creds.toPath())) {
      GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
      GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
          http, JSON_FACTORY, secrets, SCOPES)
          .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
          .setAccessType("offline")
          .build();
      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
      var cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
      return new Gmail.Builder(http, JSON_FACTORY, cred).setApplicationName("InjuryTime").build();
    }
  }

  private GmailAuth() {}
}


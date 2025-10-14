package org.injurytime.ingest.connectors;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import org.injurytime.ingest.api.SourceConnector;
import org.injurytime.ingest.api.RawItem;
import org.injurytime.ingest.api.SourceConnector;
import org.jsoup.Jsoup;

import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GmailConnector implements SourceConnector {

    private static final String APPLICATION_NAME = "InjuryTime-Ingest";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final Path credentialsJson;
    private final Path tokenDir;
    private final String query;

    public GmailConnector(Path credentialsJson, Path tokenDir, String query)
    {
        this.credentialsJson = credentialsJson;
        this.tokenDir = tokenDir;
        this.query = query;
    }

    @Override
    public String id()
    {
        return "gmail";
    }

    private Gmail service() throws Exception
    {
        final NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets secrets;
        try (var in = java.nio.file.Files.newInputStream(credentialsJson))
        {
            secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                http, JSON_FACTORY, secrets, Collections.singletonList(GmailScopes.GMAIL_READONLY))
                .setDataStoreFactory(new FileDataStoreFactory(tokenDir.toFile()))
                .setAccessType("offline").build();
        var receiver = new LocalServerReceiver.Builder().setPort(0).build();
        var cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        return new Gmail.Builder(http, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();
    }

    @Override
    public List<RawItem> fetchNew() throws Exception
    {
        var svc = service();
        ListMessagesResponse res = svc.users().messages().list("me").setQ(query).setMaxResults(50L).execute();
        List<RawItem> out = new ArrayList<>();
        if (res.getMessages() == null)
        {
            return out;
        }
        for (Message m : res.getMessages())
        {
            Message full = svc.users().messages().get("me", m.getId()).setFormat("full").execute();
            String subject = null, html = null, text = null, messageId = null;
            for (MessagePartHeader h : full.getPayload().getHeaders())
            {
                if ("Subject".equalsIgnoreCase(h.getName()))
                {
                    subject = h.getValue();
                }
                if ("Message-Id".equalsIgnoreCase(h.getName()))
                {
                    messageId = h.getValue();
                }
            }
            html = extractHtml(full.getPayload());
            if (html != null)
            {
                text = Jsoup.parse(html).text();
            }
            out.add(new RawItem("gmail", full.getId(), messageId, subject, html, text));
        }
        return out;
    }

    private String extractHtml(MessagePart part) throws Exception
    {
        if (part == null)
        {
            return null;
        }
        if ("text/html".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null)
        {
            return new String(java.util.Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null)
        {
            for (MessagePart p : part.getParts())
            {
                String s = extractHtml(p);
                if (s != null)
                {
                    return s;
                }
            }
        }
        if (part.getBody() != null && part.getBody().getData() != null)
        {
            return new String(java.util.Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        return null;
    }
}

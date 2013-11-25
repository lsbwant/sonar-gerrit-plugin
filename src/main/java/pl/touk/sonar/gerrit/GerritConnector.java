package pl.touk.sonar.gerrit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerritConnector {
    private final static Logger LOG = LoggerFactory.getLogger(GerritConnector.class);
    private static final String GET_LIST_FILES_URL_FORMAT = "/a/changes/%s/revisions/%s/files/";
    private static int REQUEST_COUNTER = 0;
    private String host;
    private int port;
    private String username;
    private String password;
    private HttpHost httpHost;
    private CredentialsProvider credentialsProvider;
    private CloseableHttpClient httpClient;
    private HttpClientContext httpClientContext;
    private DigestScheme digestScheme;
    private BasicAuthCache basicAuthCache;

    public GerritConnector(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        createHttpContext();
    }

    @NotNull
    public String listFiles(String changeId, String revisionId) throws URISyntaxException, IOException {
        URI uri = new URIBuilder().setPath(String.format(GET_LIST_FILES_URL_FORMAT, changeId, revisionId)).build();
        HttpGet httpGet = new HttpGet(uri);
        CloseableHttpResponse httpResponse = logAndExecute(httpGet);
        return consumeAndLogEntity(httpResponse);
    }

    private void createHttpContext() {
        httpClient = HttpClients.createDefault();
        httpHost = new HttpHost(host, port);
        basicAuthCache = new BasicAuthCache();
        digestScheme = new DigestScheme();
        basicAuthCache.put(httpHost, digestScheme);
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            new AuthScope(host, port),
            new UsernamePasswordCredentials(username, password));
        httpClientContext = HttpClientContext.create();
        httpClientContext.setCredentialsProvider(credentialsProvider);
//        httpClientContext.setAuthSchemeRegistry(digestScheme);
        httpClientContext.setAuthCache(basicAuthCache);
    }

    @NotNull
    private CloseableHttpResponse logAndExecute(@NotNull HttpRequestBase request) throws IOException {
        LOG.info("Request  {}: {} to {}", new Object[] {REQUEST_COUNTER++, request.getMethod(), request.getURI().toString()});
        CloseableHttpResponse httpResponse = httpClient.execute(httpHost, request, httpClientContext);
        LOG.info("Response {}: {}", REQUEST_COUNTER, httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    @NotNull
    private String consumeAndLogEntity(@NotNull CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            LOG.debug("Entity {}: no entity", REQUEST_COUNTER);
            return StringUtils.EMPTY;
        }
        String content = EntityUtils.toString(response.getEntity());
        LOG.debug("Entity {}: {}", REQUEST_COUNTER, content);
        return content;
    }


}

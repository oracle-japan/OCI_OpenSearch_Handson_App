package com.example.myproject;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

@Path("/accounts")
@RequestScoped
public class AccountResource {
    private String openSearchEndpoint;
    private final CredentialsProvider credentialsProvider;

    @Inject
    public AccountResource(@ConfigProperty(name = "oci.opensearch.api.endpoint") String openSearchEndpoint,
            @ConfigProperty(name = "oci.opensearch.credential.username") String username,
            @ConfigProperty(name = "oci.opensearch.credential.password") String password) {
        this.openSearchEndpoint = openSearchEndpoint;
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    @Path("/search/{inputTerm}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@PathParam("inputTerm") String inputTerm) {
        // Create a client.
        RestClientBuilder builder = RestClient.builder(HttpHost.create(openSearchEndpoint))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        try (RestHighLevelClient client = new RestHighLevelClient(builder)) {
            // Build search request
            SearchRequest searchRequest = new SearchRequest("accounts");

            // Build SearchSource
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.queryStringQuery(inputTerm));
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(100);
            searchRequest.source(searchSourceBuilder);

            // Search
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return Response.status(Response.Status.OK).entity(searchResponse.toString()).build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

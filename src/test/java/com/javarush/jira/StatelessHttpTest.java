package com.javarush.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql({"classpath:db/changelog-h2.sql", "classpath:data.sql"})
class StatelessHttpTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;

    @Test void realHttpRequestsNeverCreateSessionCookies() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String base = "http://localhost:" + port;
        var login = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"user@gmail.com\",\"password\":\"password\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode());
        assertFalse(login.headers().allValues("set-cookie").toString().contains("JSESSIONID"));
        String token = json.readTree(login.body()).get("accessToken").asText();
        var missing = client.send(HttpRequest.newBuilder(URI.create(base + "/static/missing.css"))
                .header("Cookie", "JR_ACCESS=" + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, missing.statusCode());
        assertTrue(missing.headers().allValues("set-cookie").isEmpty(), "A missing resource must not clear login cookies");
        for (String path : new String[]{"/", "/?lang=en", "/view/tree", "/ui/profile"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create(base + path))
                    .header("Cookie", "JR_ACCESS=" + token).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertFalse(response.headers().allValues("set-cookie").toString().contains("JSESSIONID"), path);
            assertTrue(response.body().contains("name=\"access-token\""), path);
            assertFalse(response.body().contains("action=\"/ui/logout\""));
        }
        String[][] forms = {
                {"/ui/profile", "mailNotifications=assigned", "200"},
                {"/ui/user", "email=user%40gmail.com&firstName=userFirstName&lastName=userLastName&displayName=userDisplayName", "302"},
                {"/ui/user", "email=invalid&firstName=", "302"},
                {"/ui/user/change-password", "oldPassword=wrong&newPassword=another-password", "302"},
                {"/ui/user/change-password", "oldPassword=password&newPassword=another-password", "302"}
        };
        for (String[] form : forms) {
            var response = client.send(HttpRequest.newBuilder(URI.create(base + form[0]))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form[1])).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(Integer.parseInt(form[2]), response.statusCode(), form[0]);
            assertFalse(response.headers().allValues("set-cookie").toString().contains("JSESSIONID"), form[0]);
            String location = response.headers().firstValue("location").orElse("");
            assertFalse(location.contains("accessToken"), "Redirect must not expose the token");
            assertFalse(location.contains(token));
        }
        var revoked = client.send(HttpRequest.newBuilder(URI.create(base + "/api/profile"))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, revoked.statusCode());
    }
}

/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import io.restassured.http.Header;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test base for a few common things.
 * The heavy lifting of mock-setup is done in the {@link TestLifecycleManager}
 */
abstract class AbstractITest {

    static Header authHeader;       // User with access rights
    static Header authRbacNoAccess; // Hans Dampf has no rbac access rights
    static Header authHeaderNoAccount; // Account number is empty

    static String accountId;
    static String orgId;

    static final String API_BASE_V1_0 = "/api/policies/v1.0";
    static final String API_BASE_V1 = "/api/policies/v1";
    public ClientAndServer mockServer;

    @Inject
    EntityManager entityManager;

    @BeforeAll
    static void setupRhId() {
        // provide rh-id
        String rhid = HeaderHelperTest.getStringFromFile("rhid.txt", false);
        authHeader = new Header("x-rh-identity", rhid);
        accountId = getAccountId(rhid.trim());
        orgId = getOrgId(rhid.trim());
        rhid = HeaderHelperTest.getStringFromFile("rhid_hans.txt", false);
        authRbacNoAccess = new Header("x-rh-identity", rhid);
        rhid = HeaderHelperTest.getStringFromFile("rhid_no_account.txt", false);
        authHeaderNoAccount = new Header("x-rh-identity", rhid);
    }

    static String getAccountId(String identity) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(new String(Base64.getDecoder().decode(identity), StandardCharsets.UTF_8));
            return node.get("identity").get("account_number").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static String getOrgId(String identity) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(new String(Base64.getDecoder().decode(identity), StandardCharsets.UTF_8));
            return node.get("identity").get("org_id").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    void extractAndCheck(Map<String, String> links, String rel, int limit, int offset) {
        String url = links.get(rel);
        assertNotNull(url, "Rel [" + rel + "] not found");
        String tmp = String.format("limit=%d&offset=%d", limit, offset);
        assertTrue(url.endsWith(tmp), "Url for rel [" + rel + "] should end in [" + tmp + "], but was [" + url + "]");
    }

    long countPoliciesInDB() {
        Query q = entityManager.createQuery("SELECT count(p) FROM Policy p WHERE p.orgId = :orgId");
        q.setParameter("orgId", "org-id-1234");
        return (long) q.getSingleResult();
    }
}

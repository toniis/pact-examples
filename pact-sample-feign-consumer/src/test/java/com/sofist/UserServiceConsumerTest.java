package com.sofist;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "userservice", port = "8888")
@SpringBootTest({
        // overriding provider address
        "userservice.ribbon.listOfServers: localhost:8888"
})
class UserServiceConsumerTest {

    public static final String ID = "42";

    @Autowired
    private UserClient userClient;

    @Pact(state = "provider accepts a new person", provider = "userservice", consumer = "userclient")
    RequestResponsePact createPersonPact(PactDslWithProvider builder) {

        return builder
                .given("provider accepts a new person")
                .uponReceiving("a request to POST a person")
                .path("/user-service/users")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("firstName", dummyUser().getFirstName())
                        .stringType("lastName", dummyUser().getLastName())
                )
                .willRespondWith()
                .status(201)
                .matchHeader("Content-Type", "application/json;charset=UTF-8")
                .body(new PactDslJsonBody()
                        .integerType("id", Long.valueOf(ID)))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPersonPact")
    void verifyCreatePersonPact() {
        IdObject id = userClient.createUser(dummyUser());
        assertThat(id.getId()).isEqualTo(Long.valueOf(ID));
    }

    @Pact(state = "person " + ID + " exists", provider = "userservice", consumer = "userclient")
    RequestResponsePact updatePersonPact(PactDslWithProvider builder) {
        return builder
                .given("person " + ID + " exists")
                .uponReceiving("a request to PUT a person")
                .path("/user-service/users/" + ID)
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("firstName", dummyUser().getFirstName() + " updated")
                        .stringType("lastName", dummyUser().getLastName() + " updated")
                )
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json;charset=UTF-8")
                .body(new PactDslJsonBody()
                        .stringType("firstName", dummyUser().getFirstName() + " updated")
                        .stringType("lastName", dummyUser().getLastName() + " updated"))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "updatePersonPact")
    void verifyUpdatePersonPact() {
        User user = dummyUser();
        user.setFirstName(user.getFirstName() + " updated");
        user.setLastName(user.getLastName() + " updated");
        User updatedUser = userClient.updateUser(Long.valueOf(ID), user);
        assertThat(updatedUser.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(user.getLastName());
    }

    static User dummyUser() {
        User user = new User();
        user.setFirstName("Zaphod");
        user.setLastName("Beeblebrox");
        return user;
    }

}

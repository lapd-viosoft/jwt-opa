/*
 * Copyright (c) 2020 kapsules.io.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kapsules.jwt.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.kapsules.jwt.AbstractTestBase;
import io.kapsules.jwt.ApiTokenAuthentication;
import io.kapsules.jwt.ApiTokenAuthenticationFactory;
import io.kapsules.jwt.JwtTokenProvider;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiTokenAuthenticationTest extends AbstractTestBase {

  @Autowired
  JwtTokenProvider provider;

  @Autowired
  ApiTokenAuthenticationFactory factory;

  String token;
  Authentication auth;

  @BeforeEach
  void setUp() {
    token = provider.createToken("alice", Lists.list("USER", "ADMIN"));
    auth = factory.createAuthentication(token);
  }

  @Test
  void getAuthorities() {
    assertThat(auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("USER", "ADMIN");
  }

  @Test
  void getCredentials() {
    assertThat(auth.getCredentials()).isEqualTo(token);
  }

  @Test
  void getDetails() {
    DecodedJWT details = (DecodedJWT) auth.getDetails();
    DecodedJWT jwt = provider.decode(token);

    assertThat(details.getSubject()).isEqualTo(jwt.getSubject());
    assertThat(details.getSignature()).isEqualTo(jwt.getSignature());
    assertThat(details.getIssuer()).isEqualTo(jwt.getIssuer());
  }

  @Test
  void getPrincipal() {
    assertThat(((Principal) auth.getPrincipal()).getName()).isEqualTo("alice");
  }

  @Test
  void isAuthenticated() {
    assertThat(auth.isAuthenticated()).isTrue();

    String tok2 = provider.createToken("bob", Lists.list("PAINTER"));
    Authentication auth2 = factory.createAuthentication(tok2);
    assertThat(auth.isAuthenticated()).isTrue();
  }

  @Test
  void isAuthenticatedFailsForBogus() {
    ApiTokenAuthentication invalid = factory.createAuthentication(
        token.replace("s", "5").replace("e", "3")
    );
    assertThat(invalid.isAuthenticated()).isFalse();

    invalid = factory.createAuthentication("bogus secret and a half");
    assertThat(invalid.isAuthenticated()).isFalse();
  }

  @Test
  void setAuthenticated() {
    assertThrows(IllegalArgumentException.class, () -> auth.setAuthenticated(true));
  }
}

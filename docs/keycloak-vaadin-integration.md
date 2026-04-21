# Integração Keycloak + Vaadin (Spring Boot)

## Visão geral

Este documento descreve como integrar autenticação OAuth2/OIDC via **Keycloak** em uma aplicação **Spring Boot 4** com **Vaadin 25**, criando uma página de login personalizada que redireciona o usuário para o Keycloak.

### Fluxo de autenticação

```
Usuário acessa "/"
      │
      ▼
Spring Security verifica sessão
      │ não autenticado
      ▼
Redireciona para "/login" (LoginView Vaadin)
      │
      ▼
Usuário clica "Entrar com Keycloak"
      │
      ▼
Redireciona para "/oauth2/authorization/keycloak"
      │
      ▼
Spring Security inicia fluxo OAuth2 Authorization Code
      │
      ▼
Keycloak exibe formulário de login
      │ credenciais válidas
      ▼
Keycloak redireciona de volta: "/login/oauth2/code/keycloak"
      │
      ▼
Spring Security valida o código, cria sessão
      │
      ▼
Usuário autenticado → redireciona para "/"
```

---

## 1. Dependências (pom.xml)

Adicione as duas dependências ao `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Por que essas duas?**

- `spring-boot-starter-security`: ativa o Spring Security na aplicação — sem ele, nenhuma rota é protegida.
- `spring-boot-starter-oauth2-client`: habilita o fluxo OAuth2 Authorization Code, que é o protocolo usado para delegar autenticação a um servidor externo (Keycloak). Inclui o endpoint `/oauth2/authorization/{registrationId}` e o callback `/login/oauth2/code/{registrationId}`.

---

## 2. Configuração do provider Keycloak (application.yml)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: ${KEYCLOAK_CLIENT_ID:investment-advisor}
            client-secret: ${KEYCLOAK_CLIENT_SECRET:}
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          keycloak:
            issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9080/realms/master}
            user-name-attribute: preferred_username
```

**Explicação de cada campo:**

| Campo | Descrição |
|---|---|
| `client-id` | ID do client cadastrado no Keycloak |
| `client-secret` | Secret gerado pelo Keycloak (aba Credentials do client) |
| `scope` | Escopos OIDC solicitados. `openid` é obrigatório; `profile` e `email` trazem dados do usuário |
| `authorization-grant-type` | Tipo de fluxo OAuth2. `authorization_code` é o padrão para aplicações web com servidor |
| `redirect-uri` | URL de callback onde o Keycloak retorna após o login. O placeholder `{baseUrl}` é resolvido automaticamente pelo Spring |
| `issuer-uri` | URL base do realm no Keycloak. O Spring usa esse endereço para descobrir automaticamente os endpoints OIDC (`.well-known/openid-configuration`) |
| `user-name-attribute` | Campo do token JWT usado como identificador do usuário logado |

**Variáveis de ambiente esperadas:**

```
KEYCLOAK_CLIENT_ID      → ID do client (padrão: investment-advisor)
KEYCLOAK_CLIENT_SECRET  → Secret gerado pelo Keycloak
KEYCLOAK_ISSUER_URI     → URL do realm (padrão: http://localhost:9080/realms/master)
```

---

## 3. SecurityConfig (Spring Security + Vaadin)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer
                .loginView(LoginView.class)
                .defaultSuccessUrl("/", true)
        );

        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
        );

        http.logout(logout -> logout
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
        );

        return http.build();
    }
}
```

**Por que `VaadinSecurityConfigurer` e não `VaadinWebSecurity`?**

O `VaadinWebSecurity` (classe que estendia `WebSecurityConfigurerAdapter`) foi removido no Vaadin 25. A nova API usa `VaadinSecurityConfigurer`, um `AbstractHttpConfigurer` aplicado via `http.with(...)`. Ele configura internamente:

- CSRF compatível com as requisições Vaadin (Push, heartbeat, etc.)
- Cache de requisições para redirecionar o usuário à página original após o login
- Controle de acesso de navegação baseado nas anotações `@PermitAll`, `@RolesAllowed`, `@AnonymousAllowed`
- Tratamento de `AccessDeniedException` para views Vaadin

**Por que `.loginPage("/login")` no `oauth2Login`?**

Sem essa linha, o Spring Security exibe sua própria página genérica "Login with OAuth 2.0" em vez do nosso `LoginView` Vaadin. Configurar `loginPage("/login")` faz com que qualquer requisição não autenticada (seja de form login ou de OAuth2) seja redirecionada para a nossa view personalizada.

---

## 4. LoginView (página de login Vaadin)

```java
@Route("login")
@PageTitle("Login — Investment Advisor")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // ... componentes visuais ...

        Button loginButton = new Button("Entrar com Keycloak",
                e -> UI.getCurrent().getPage().setLocation("/oauth2/authorization/keycloak"));

        // ...
    }
}
```

**Pontos importantes:**

- `@AnonymousAllowed`: obrigatório. Sem essa anotação, o Spring Security redirecionaria `/login` para `/login` infinitamente (loop de redirecionamento), pois a própria view de login estaria protegida.
- `UI.getCurrent().getPage().setLocation(...)`: forma correta de fazer redirect de página inteira no Vaadin. Diferente de `navigate()`, que faz navegação interna do Vaadin Router — aqui precisamos sair completamente para o endpoint do Spring Security.
- `/oauth2/authorization/keycloak`: endpoint registrado automaticamente pelo Spring Security quando um client com registration ID `keycloak` está configurado. Ele inicia o fluxo OAuth2 redirecionando ao Keycloak.

---

## 5. Protegendo as views (AnalysisView)

```java
@Route("")
@PageTitle("Investment Advisor")
@PermitAll
public class AnalysisView extends VerticalLayout {
    // ...
    public AnalysisView(GetTechnicalAnalysisUseCase useCase, AuthenticationContext authContext) {
        String username = authContext.getPrincipalName().orElse("Usuário");
        Button logoutButton = new Button("Sair", e -> authContext.logout());
        // ...
    }
}
```

**Anotações de controle de acesso no Vaadin:**

| Anotação | Comportamento |
|---|---|
| `@AnonymousAllowed` | Qualquer um acessa, autenticado ou não |
| `@PermitAll` | Apenas usuários autenticados, qualquer papel |
| `@RolesAllowed("ROLE_ADMIN")` | Apenas usuários com o papel especificado |
| *(sem anotação)* | Acesso negado a todos (comportamento padrão com segurança ativa) |

**`AuthenticationContext`** é um bean Vaadin (injetado via construtor) que oferece:
- `getPrincipalName()` → nome do usuário logado (campo `preferred_username` do token)
- `logout()` → invalida a sessão Vaadin + sessão Spring Security corretamente

---

## 6. Realms no Keycloak — por que não usar o `master`

Ao testar pela primeira vez, é comum usar o realm `master` e acabar logando como `admin`. Isso acontece porque o `master` é o **realm de administração do próprio Keycloak** — o único usuário cadastrado lá é o administrador criado na instalação.

**O `master` realm nunca deve ser usado para aplicações.** Ele existe somente para gerenciar o Keycloak em si (criar outros realms, configurar providers, etc.).

### Estrutura correta de realms

```
Keycloak
├── master          ← administração do Keycloak (não use para apps)
└── investment-advisor  ← realm da sua aplicação
        ├── Clients
        │   └── investment-advisor (o client OAuth2)
        └── Users
            └── seus usuários da aplicação
```

### Criando um realm dedicado

1. No Admin Console, clique no dropdown do realm (canto superior esquerdo)
2. **Create realm**
3. **Realm name:** `investment-advisor`
4. Salve — o Keycloak muda automaticamente para o novo realm

### Configurando o Client no novo realm

Ainda no realm `investment-advisor`:

1. **Clients → Create client**
2. **Client type:** `OpenID Connect`
3. **Client ID:** `investment-advisor` (deve bater com o `client-id` do `application.yml`)
4. Habilite **Client authentication** (torna o client confidential — exige secret)
5. Em **Valid redirect URIs:**
   ```
   http://localhost:8086/login/oauth2/code/keycloak
   ```
6. Em **Web origins:**
   ```
   http://localhost:8086
   ```
7. Salve → aba **Credentials** → copie o **Client Secret**
8. Defina o secret na variável `KEYCLOAK_CLIENT_SECRET` ou diretamente no `application.yml`

**Por que `Valid redirect URIs` é crítico?**

O Keycloak valida que a URI de callback informada pelo cliente OAuth2 está na lista permitida. Se não estiver, o Keycloak rejeita a requisição com "Invalid redirect_uri". Isso é uma proteção contra ataques de redirecionamento aberto.

### Criando usuários da aplicação

Com o realm dedicado, crie usuários específicos da aplicação (sem acesso ao painel admin):

1. **Users → Create user**
2. Preencha **Username** e **Email**
3. Aba **Credentials → Set password** (desmarque "Temporary" para não forçar troca no primeiro login)

### Atualizando o `application.yml`

Altere o `issuer-uri` para apontar para o novo realm:

```yaml
provider:
  keycloak:
    issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9080/realms/investment-advisor}
```

O Spring usa esse URI para descobrir automaticamente todos os endpoints OIDC do realm via `/.well-known/openid-configuration`.

---

## Resumo dos arquivos modificados

| Arquivo | O que foi feito |
|---|---|
| `pom.xml` | Adicionadas dependências `spring-security` e `oauth2-client` |
| `application.yml` | Configurado provider e registration do Keycloak |
| `SecurityConfig.java` | Criado — configura `VaadinSecurityConfigurer` + OAuth2 login |
| `LoginView.java` | Criado — página Vaadin com botão "Entrar com Keycloak" |
| `AnalysisView.java` | Adicionado `@PermitAll`, nome do usuário e botão de logout |

---

## Referências

- [Vaadin Security Documentation](https://vaadin.com/docs/latest/security)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth2 Authorization Code Flow (RFC 6749)](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1)

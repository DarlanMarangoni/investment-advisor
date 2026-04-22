# Tema customizado do Keycloak — Investment Advisor

Documentação do tema visual criado para a página de login do Keycloak, alinhado ao estilo da interface Vaadin da aplicação.

---

## O que foi feito

O tema replica a aparência do `LoginView` Vaadin:

- Fundo cinza claro (`#f3f4f6`) — equivalente ao `--lumo-contrast-5pct` do Vaadin Lumo
- Card branco centralizado com `border-radius: 12px` e sombra suave
- Título azul Lumo (`hsl(214, 100%, 48%)`)
- Botão primário azul full-width
- Textos traduzidos para PT-BR
- Título da página substituído por **Investment Advisor**

---

## Estrutura de arquivos

```
keycloak/themes/investment-advisor/
└── login/
    ├── theme.properties                        ← herda do keycloak.v2, carrega o CSS
    ├── template.ftl                            ← layout HTML da página de login
    ├── resources/
    │   └── css/
    │       └── login.css                       ← estilos visuais
    └── messages/
        ├── messages_en.properties              ← sobrescreve textos em inglês
        └── messages_pt_BR.properties           ← textos em português
```

---

## Como atualizar o tema

### 1. Edite os arquivos localmente

Os arquivos do tema estão em `keycloak/themes/investment-advisor/`. O diretório está montado como volume no container, então **não é necessário `docker cp`** — edite localmente e reinicie.

| Arquivo | O que controla |
|---|---|
| `resources/css/login.css` | Cores, layout, tipografia |
| `messages/messages_en.properties` | Textos em inglês (ex: título, labels) |
| `messages/messages_pt_BR.properties` | Textos em português |
| `theme.properties` | Tema pai, arquivos CSS carregados, locales |
| `template.ftl` | Estrutura HTML da página |

---

### 2. Reinicie o container

O Keycloak serve os arquivos CSS com `Cache-Control: max-age=2592000` (30 dias). Qualquer mudança de CSS exige reinício para o cache ser invalidado:

```bash
docker restart keycloak
```

Após reiniciar, recarregue a página com **`Ctrl+Shift+R`** (força busca do CSS novo, ignorando cache do browser).

> **Atenção:** F5 comum não é suficiente — o browser vai usar a versão cacheada. Use sempre `Ctrl+Shift+R` após mudanças de CSS.

---

### 3. Ative o tema no Admin Console (primeira vez)

Se o tema ainda não estiver ativado no realm:

1. Acesse `http://localhost:9080/admin`
2. Selecione o realm `investment-advisor`
3. Vá em **Realm Settings** → aba **Themes**
4. Em **Login Theme**, selecione `investment-advisor`
5. Clique em **Save**

---

### 4. Ative o português como idioma padrão (primeira vez)

Por padrão o Keycloak usa o idioma do browser. Para forçar PT-BR independente do browser:

1. Acesse `http://localhost:9080/admin`
2. Selecione o realm `investment-advisor`
3. Vá em **Realm Settings** → aba **Localization**
4. Ative **Internationalization Enabled**
5. Em **Supported Locales**, adicione `pt-BR`
6. Em **Default Locale**, selecione `pt-BR`
7. Clique em **Save**

> Se a página ainda aparecer em inglês após configurar, limpe os cookies do `localhost:9080` no browser — o Keycloak armazena o locale da sessão em cookie e ele pode estar fixado em `en`.

---

## Armadilhas do PatternFly 5 (Keycloak 24+)

O Keycloak 26 usa PatternFly 5, cujo CSS tem comportamentos não óbvios que causaram problemas durante o desenvolvimento deste tema.

### Propriedades lógicas vs físicas

O PF5 usa propriedades CSS **lógicas** (`padding-inline-start`, `padding-inline-end`) em vez das físicas (`padding-left`, `padding-right`). Isso significa que:

```css
/* NÃO sobrescreve padding-inline-start/end do PF */
.pf-v5-c-login__container {
    padding: 1rem !important;
}

/* Necessário usar a propriedade lógica também */
.pf-v5-c-login__container {
    padding: 1rem !important;
    padding-inline: 1rem !important;
}
```

O `.pf-v5-c-login__container` tem por padrão `padding-inline: 6.125rem` (~98px de cada lado). Sem sobrescrever com `padding-inline`, o card fica com apenas ~284px de largura útil mesmo com `max-width: 480px`.

### Grid de 2 colunas

Em viewports grandes, o PF5 aplica no `.pf-v5-c-login__container`:

```css
display: grid;
grid-template-areas: "main header" "main footer" "main .";
grid-template-columns: 34rem minmax(auto, 34rem);
```

Como o `template.ftl` deste tema não usa as áreas `header` e `footer` do grid, o elemento `main` (o card) ficava comprimido. A correção foi sobrescrever para `display: block`.

### Padding interno do card

O PF5 **não** aplica padding diretamente em `.pf-v5-c-login__main`. O espaçamento interno do card é controlado nos elementos filhos:

- `.pf-v5-c-login__main-header` — padding do cabeçalho (título)
- `.pf-v5-c-login__main-body` — padding do formulário
- `.pf-v5-c-login__main-footer` — padding do rodapé (links)

Para alterar o espaço entre a borda do card e o conteúdo, sobrescreva esses três elementos (com `padding-inline` também):

```css
.pf-v5-c-login__main-header,
.pf-v5-c-login__main-body,
.pf-v5-c-login__main-footer {
    padding: 1.5rem !important;
    padding-inline: 1.5rem !important;
}
```

### Centralização

O PF5 já centraliza o card via `display: flex; justify-content: center; align-items: center; min-height: 100vh` no `.pf-v5-c-login`. **Não é necessário** adicionar flexbox/grid no `body` para centralizar — isso cria conflito com o layout do PF.

---

## CSS atual explicado

```css
body {
    /* Só fundo — PF já faz a centralização */
    background-color: var(--ia-bg-page) !important;
    margin: 0 !important;
}

.pf-v5-c-login {
    background-color: var(--ia-bg-page) !important;
    /* Zera as variáveis de padding de 6.125rem do container */
    --pf-v5-c-login__container--PaddingLeft: 0px;
    --pf-v5-c-login__container--PaddingRight: 0px;
}

.pf-v5-c-login__container {
    /* Substitui grid de 2 colunas por bloco simples */
    display: block !important;
    max-width: 480px !important;
    width: 100% !important;
    padding: 1rem !important;
    padding-inline: 1rem !important; /* necessário por causa das propriedades lógicas do PF */
}

.pf-v5-c-login__main {
    /* Card visual — sem padding próprio (controlado pelos filhos) */
    background: var(--ia-bg-card) !important;
    border-radius: 12px !important;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12) !important;
    padding: 0 !important;
}

.pf-v5-c-login__main-header,
.pf-v5-c-login__main-body,
.pf-v5-c-login__main-footer {
    /* Espaçamento interno — PF usa lógicas, precisamos de ambas */
    padding: 1.5rem !important;
    padding-inline: 1.5rem !important;
}
```

---

## Referências

- [Keycloak — Server Developer Guide: Themes](https://www.keycloak.org/docs/latest/server_development/#_themes)
- [PatternFly 5 CSS Variables](https://www.patternfly.org/tokens/all-patternfly-tokens)
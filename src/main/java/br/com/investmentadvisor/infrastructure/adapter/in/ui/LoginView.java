package br.com.investmentadvisor.infrastructure.adapter.in.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login — Investment Advisor")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("padding", "3rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 20px rgba(0,0,0,0.12)")
                .set("text-align", "center")
                .set("min-width", "360px");

        H1 title = new H1("Investment Advisor");
        title.getStyle()
                .set("margin-top", "0")
                .set("margin-bottom", "0.25rem")
                .set("color", "var(--lumo-primary-color)");

        Paragraph subtitle = new Paragraph("Análise técnica de ações da B3");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "0")
                .set("margin-bottom", "2rem");

        Button loginButton = new Button("Entrar com Keycloak",
                e -> UI.getCurrent().getPage().setLocation("/oauth2/authorization/keycloak"));
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        loginButton.setWidth("100%");

        Span footer = new Span("Autenticação gerenciada via Keycloak");
        footer.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-top", "1.5rem")
                .set("display", "block");

        card.add(title, subtitle, loginButton, footer);
        add(card);
    }
}
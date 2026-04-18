package br.com.investmentadvisor.infrastructure.adapter.in.ui;

import br.com.investmentadvisor.domain.model.TechnicalAnalysis;
import br.com.investmentadvisor.domain.model.TechnicalSignal;
import br.com.investmentadvisor.domain.port.in.FetchQuotesUseCase;
import br.com.investmentadvisor.domain.port.in.GetTechnicalAnalysisUseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Route("")
@PageTitle("Investment Advisor")
public class AnalysisView extends VerticalLayout {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GetTechnicalAnalysisUseCase getTechnicalAnalysisUseCase;
    private final FetchQuotesUseCase fetchQuotesUseCase;
    private final Grid<TechnicalAnalysis> grid;

    public AnalysisView(GetTechnicalAnalysisUseCase getTechnicalAnalysisUseCase,
                        FetchQuotesUseCase fetchQuotesUseCase) {
        this.getTechnicalAnalysisUseCase = getTechnicalAnalysisUseCase;
        this.fetchQuotesUseCase = fetchQuotesUseCase;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Análise Técnica — B3");

        Button refreshButton = new Button("Atualizar cotações", e -> fetchAndRefresh());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button reloadButton = new Button("Recarregar tabela", e -> loadData());
        reloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout toolbar = new HorizontalLayout(refreshButton, reloadButton);
        toolbar.setAlignItems(Alignment.CENTER);

        grid = buildGrid();

        add(title, toolbar, grid);
        expand(grid);

        loadData();
    }

    private Grid<TechnicalAnalysis> buildGrid() {
        Grid<TechnicalAnalysis> g = new Grid<>(TechnicalAnalysis.class, false);

        g.addColumn(TechnicalAnalysis::ticker)
                .setHeader("Ticker")
                .setWidth("100px")
                .setFlexGrow(0);

        g.addColumn(a -> format(a.price()))
                .setHeader("Preço (R$)")
                .setWidth("120px")
                .setFlexGrow(0);

        g.addColumn(a -> format(a.sma20()))
                .setHeader("SMA 20")
                .setWidth("110px")
                .setFlexGrow(0);

        g.addColumn(a -> format(a.sma50()))
                .setHeader("SMA 50")
                .setWidth("110px")
                .setFlexGrow(0);

        g.addColumn(a -> format(a.rsi()))
                .setHeader("RSI (14)")
                .setWidth("100px")
                .setFlexGrow(0);

        g.addComponentColumn(a -> signalBadge(a.signal()))
                .setHeader("Sinal")
                .setWidth("110px")
                .setFlexGrow(0);

        g.addColumn(a -> a.analyzedAt() != null ? a.analyzedAt().format(FORMATTER) : "—")
                .setHeader("Analisado em")
                .setFlexGrow(1);

        g.setSizeFull();
        return g;
    }

    private void loadData() {
        try {
            grid.setItems(getTechnicalAnalysisUseCase.analyzeAll());
        } catch (Exception e) {
            showError("Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void fetchAndRefresh() {
        try {
            fetchQuotesUseCase.fetchAndStoreQuotes();
            loadData();
            Notification n = Notification.show("Cotações atualizadas com sucesso!");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            n.setDuration(3000);
        } catch (Exception e) {
            showError("Erro ao buscar cotações: " + e.getMessage());
        }
    }

    private Span signalBadge(TechnicalSignal signal) {
        if (signal == null) return new Span("—");

        Span badge = new Span(signal.name());
        badge.getStyle()
                .set("border-radius", "4px")
                .set("padding", "2px 8px")
                .set("font-weight", "bold")
                .set("font-size", "0.85em")
                .set("color", "white");

        String color = switch (signal) {
            case BUY  -> "#2e7d32";
            case SELL -> "#c62828";
            case HOLD -> "#f57c00";
        };
        badge.getStyle().set("background-color", color);
        return badge;
    }

    private String format(BigDecimal value) {
        return value != null ? value.toPlainString() : "—";
    }

    private void showError(String message) {
        Notification n = Notification.show(message);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setDuration(5000);
    }
}

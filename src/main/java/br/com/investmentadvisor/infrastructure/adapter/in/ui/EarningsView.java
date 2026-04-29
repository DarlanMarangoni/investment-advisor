package br.com.investmentadvisor.infrastructure.adapter.in.ui;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.in.GetEarningsAnalysesUseCase;
import br.com.investmentadvisor.domain.port.in.UploadEarningsReportUseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Route("")
@PageTitle("Resumo de Relátorios — Investment Advisor")
@PermitAll
public class EarningsView extends VerticalLayout {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] QUARTER_LABELS = {"01", "02", "03", "04"};

    private final UploadEarningsReportUseCase uploadEarningsReportUseCase;
    private final GetEarningsAnalysesUseCase getEarningsAnalysesUseCase;

    private final TextField tickerField = new TextField("Ticker");
    private final Select<Integer> referenceQuarterSelect = new Select<>();
    private final IntegerField referenceYearField = new IntegerField("Ano");
    private final ByteArrayOutputStream uploadedData = new ByteArrayOutputStream();
    private String uploadedFileName;
    private final Upload upload = new Upload();
    private final Button analyzeButton = new Button("Analisar com IA");
    private final ProgressBar progressBar = new ProgressBar();
    private final HorizontalLayout progressRow = new HorizontalLayout();
    private final VerticalLayout resultSection = new VerticalLayout();
    private final H3 resultTitle = new H3();
    private final TextArea analysisArea = new TextArea();
    private final Grid<EarningsAnalysis> historyGrid = new Grid<>(EarningsAnalysis.class, false);

    public EarningsView(UploadEarningsReportUseCase uploadEarningsReportUseCase,
                        GetEarningsAnalysesUseCase getEarningsAnalysesUseCase,
                        AuthenticationContext authContext) {
        this.uploadEarningsReportUseCase = uploadEarningsReportUseCase;
        this.getEarningsAnalysesUseCase = getEarningsAnalysesUseCase;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        VerticalLayout historyCard = buildHistorySection();
        add(
                buildToolbar(authContext),
                buildUploadCard(),
                buildResultSection(),
                historyCard
        );
        expand(historyCard);
        loadHistory();
    }

    private HorizontalLayout buildToolbar(AuthenticationContext authContext) {
        H2 title = new H2("Análise de relatórios  — B3");
        title.getStyle().set("margin", "0");

        String username = authContext.getPrincipalName().orElse("Usuário");
        Span userLabel = new Span("Olá, " + username);
        userLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button logoutButton = new Button("Sair", e -> authContext.logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout toolbar = new HorizontalLayout(title, userLabel, logoutButton);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setFlexGrow(1, userLabel);
        toolbar.setWidthFull();
        return toolbar;
    }

    private VerticalLayout buildUploadCard() {
        tickerField.setPlaceholder("Ex: PETR4");
        tickerField.setWidth("150px");

        referenceQuarterSelect.setLabel("Trimestre");
        referenceQuarterSelect.setItems(1, 2, 3, 4);
        referenceQuarterSelect.setItemLabelGenerator(q -> QUARTER_LABELS[q - 1]);
        referenceQuarterSelect.setValue((LocalDate.now().getMonthValue() - 1) / 3 + 1);
        referenceQuarterSelect.setWidth("130px");

        referenceYearField.setValue(LocalDate.now().getYear());
        referenceYearField.setMin(2000);
        referenceYearField.setMax(2100);
        referenceYearField.setStepButtonsVisible(true);
        referenceYearField.setWidth("140px");

        upload.setReceiver((filename, mimeType) -> {
            uploadedData.reset();
            uploadedFileName = filename;
            return uploadedData;
        });
        upload.setAcceptedFileTypes("application/pdf", ".pdf");
        upload.setMaxFileSize(30 * 1024 * 1024);
        upload.setDropAllowed(true);
        upload.addSucceededListener(e ->
                Notification.show("PDF carregado: " + e.getFileName(), 3000, Notification.Position.BOTTOM_START));
        upload.addFailedListener(e -> showError("Falha no upload: " + e.getReason().getMessage()));

        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.addClickListener(e -> handleAnalyze());

        progressBar.setIndeterminate(true);
        Span progressLabel = new Span("Analisando com IA... isso pode levar alguns segundos.");
        progressLabel.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.9em");
        progressRow.add(progressBar, progressLabel);
        progressRow.setAlignItems(Alignment.CENTER);
        progressRow.setWidthFull();
        progressRow.setVisible(false);

        HorizontalLayout fields = new HorizontalLayout(tickerField, referenceQuarterSelect, referenceYearField);
        fields.setAlignItems(Alignment.BASELINE);

        VerticalLayout card = new VerticalLayout(
                new H3("Novo Relatório"),
                fields,
                upload,
                analyzeButton,
                progressRow
        );
        styleCard(card);
        return card;
    }

    private VerticalLayout buildResultSection() {
        analysisArea.setReadOnly(true);
        analysisArea.setWidthFull();
        analysisArea.setHeight("400px");
        analysisArea.getStyle().set("font-family", "monospace").set("font-size", "0.9em");

        Button closeBtn = new Button("Fechar", e -> resultSection.setVisible(false));
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(resultTitle, closeBtn);
        header.setAlignItems(Alignment.CENTER);
        header.setWidthFull();
        header.setFlexGrow(1, resultTitle);

        resultSection.add(header, analysisArea);
        styleCard(resultSection);
        resultSection.setVisible(false);
        return resultSection;
    }

    private VerticalLayout buildHistorySection() {
        buildHistoryGrid();

        VerticalLayout card = new VerticalLayout(new H3("Histórico de Análises"), historyGrid);
        styleCard(card);
        card.setSizeFull();
        historyGrid.setSizeFull();
        return card;
    }

    private void buildHistoryGrid() {
        historyGrid.addColumn(EarningsAnalysis::ticker)
                .setHeader("Ticker")
                .setWidth("100px")
                .setFlexGrow(0);

        historyGrid.addColumn(a -> String.format("%s/%d", QUARTER_LABELS[a.referenceQuarter() - 1], a.referenceYear()))
                .setHeader("Referência")
                .setWidth("150px")
                .setFlexGrow(0);

        historyGrid.addColumn(a -> a.analyzedAt() != null ? a.analyzedAt().format(FORMATTER) : "—")
                .setHeader("Analisado em")
                .setWidth("160px")
                .setFlexGrow(0);

        historyGrid.addColumn(a -> truncate(a.analysis(), 120))
                .setHeader("Prévia")
                .setFlexGrow(1);

        historyGrid.addComponentColumn(a -> {
            Button btn = new Button("Ver análise", e -> openAnalysisDialog(a));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return btn;
        }).setWidth("120px").setFlexGrow(0);

        historyGrid.setSizeFull();
    }

    private void handleAnalyze() {
        if (tickerField.isEmpty()) {
            showError("Informe o ticker da empresa.");
            return;
        }
        if (referenceQuarterSelect.isEmpty()) {
            showError("Selecione o trimestre de referência.");
            return;
        }
        if (referenceYearField.isEmpty()) {
            showError("Informe o ano de referência.");
            return;
        }
        if (uploadedFileName == null || uploadedFileName.isBlank()) {
            showError("Selecione um arquivo PDF antes de analisar.");
            return;
        }

        String ticker = tickerField.getValue().trim().toUpperCase();
        int quarter = referenceQuarterSelect.getValue();
        int year = referenceYearField.getValue();
        byte[] bytes = uploadedData.toByteArray();
        if (bytes.length == 0) {
            showError("Selecione um arquivo PDF antes de analisar.");
            return;
        }

        analyzeButton.setEnabled(false);
        progressRow.setVisible(true);
        resultSection.setVisible(false);

        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            try {
                EarningsAnalysis result = uploadEarningsReportUseCase.upload(bytes, ticker, quarter, year);
                ui.access(() -> {
                    showAnalysis(result);
                    loadHistory();
                    progressRow.setVisible(false);
                    analyzeButton.setEnabled(true);
                    Notification.show("Análise concluída!", 3000, Notification.Position.BOTTOM_END);
                });
            } catch (Exception ex) {
                ui.access(() -> {
                    showError("Erro na análise: " + ex.getMessage());
                    progressRow.setVisible(false);
                    analyzeButton.setEnabled(true);
                });
            }
        });
    }

    private void showAnalysis(EarningsAnalysis analysis) {
        resultTitle.setText("Análise — " + analysis.ticker()
                + " · " + QUARTER_LABELS[analysis.referenceQuarter() - 1] + "/" + analysis.referenceYear());
        analysisArea.setValue(analysis.analysis());
        resultSection.setVisible(true);
    }

    private void openAnalysisDialog(EarningsAnalysis analysis) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Análise — " + analysis.ticker()
                + " · " + QUARTER_LABELS[analysis.referenceQuarter() - 1] + "/" + analysis.referenceYear());
        dialog.setWidth("860px");
        dialog.setHeight("640px");

        TextArea area = new TextArea();
        area.setValue(analysis.analysis());
        area.setReadOnly(true);
        area.setSizeFull();
        area.getStyle().set("font-family", "monospace").set("font-size", "0.85em");

        Button closeBtn = new Button("Fechar", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(area);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private void loadHistory() {
        try {
            historyGrid.setItems(getEarningsAnalysesUseCase.findAll());
        } catch (Exception e) {
            showError("Erro ao carregar histórico: " + e.getMessage());
        }
    }

    private void styleCard(VerticalLayout layout) {
        layout.getStyle()
                .set("background", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 12px rgba(0,0,0,0.07)")
                .set("padding", "20px");
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max) + "…";
    }

    private void showError(String message) {
        Notification n = Notification.show(message, 5000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}

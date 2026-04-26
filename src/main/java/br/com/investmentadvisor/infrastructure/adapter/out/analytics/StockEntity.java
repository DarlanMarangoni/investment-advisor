package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_STOCK")
public class StockEntity {

    @Id
    @SequenceGenerator(
            name = "tb_stock_id_seq",
            sequenceName = "tb_stock_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_stock_id_seq")
    @Column(name = "id")
    private Long id;
    @Column(name = "ticker")
    private String ticker;
    @Column(name = "preco")
    private BigDecimal preco;
    @Column(name = "dy")
    private BigDecimal dividendYield;
    @Column(name = "pl")
    private BigDecimal pL;
    @Column(name = "p_vp")
    private BigDecimal pVP;
    @Column(name = "p_ativos")
    private BigDecimal pAtivos;
    @Column(name = "margem_bruta")
    private BigDecimal margemBruta;
    @Column(name = "margem_ebit")
    private BigDecimal margemEbit;
    @Column(name = "marg_liquida")
    private BigDecimal margemLiquida;
    @Column(name = "p_ebit")
    private BigDecimal pEbit;
    @Column(name = "ev_ebit")
    private BigDecimal evEbit;
    @Column(name = "divida_liquida_ebit")
    private BigDecimal dividaLiquidaEbit;
    @Column(name = "div_liquida_patrimonio")
    private BigDecimal DivLiqPatri;
    @Column(name = "psr")
    private BigDecimal pSR;
    @Column(name = "p_cap_giro")
    private BigDecimal pCapGiro;
    @Column(name = "p_at_cir_liq")
    private BigDecimal pAtCirLiq;
    @Column(name = "liq_corrente")
    private BigDecimal liqCorrente;
    @Column(name = "roe")
    private BigDecimal rOE;
    @Column(name = "roa")
    private BigDecimal rOA;
    @Column(name = "roic")
    private BigDecimal rOIC;
    @Column(name = "patrimonio_ativos")
    private BigDecimal patrimonioAtivos;
    @Column(name = "passivos_ativos")
    private BigDecimal passivosAtivos;
    @Column(name = "giro_ativos")
    private BigDecimal giroAtivos;
    @Column(name = "cagr_receitas_5_anos")
    private BigDecimal cAGRReceitas5Anos;
    @Column(name = "cagr_lucros_5_anos")
    private BigDecimal cAGRLucros5Anos;
    @Column(name = "liquidez_media_diaria")
    private BigDecimal liquidezMediaDiaria;
    @Column(name = "vpa")
    private BigDecimal vPA;
    @Column(name = "lpa")
    private BigDecimal lPA;
    @Column(name = "peg_ratio")
    private BigDecimal pegRatio;
    @Column(name = "valor_mercado")
    private BigDecimal valorMercado;
    @Column(name = "dat_creation")
    @CreationTimestamp
    private LocalDateTime datCreation;
}

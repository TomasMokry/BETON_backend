package org.tomo.beton.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Bank fee charged on card payments, as a percent of the order total (e.g. 1.5 = 1.5 %). */
@Configuration
@ConfigurationProperties(prefix = "beton.card-fee")
@Data
public class CardFeeConfig {
    private BigDecimal percent = BigDecimal.ZERO;
}

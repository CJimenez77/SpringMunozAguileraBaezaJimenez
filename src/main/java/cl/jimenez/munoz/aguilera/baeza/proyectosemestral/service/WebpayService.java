package cl.jimenez.munoz.aguilera.baeza.proyectosemestral.service;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebpayService {

    private final WebpayPlus.Transaction tx;

    @Value("${transbank.webpay.return-url:http://localhost:8081/reservas/webpay-retorno}")
    private String returnUrl;

    public WebpayService(
            @Value("${transbank.webpay.commerce-code:}") String commerceCode,
            @Value("${transbank.webpay.api-key:}") String apiKey,
            @Value("${transbank.webpay.environment:TEST}") String environment) {

        if ("LIVE".equalsIgnoreCase(environment)) {
            WebpayOptions options = new WebpayOptions(commerceCode, apiKey, IntegrationType.LIVE);
            this.tx = new WebpayPlus.Transaction(options);
        } else {
            WebpayOptions options = new WebpayOptions(
                    IntegrationCommerceCodes.WEBPAY_PLUS,
                    IntegrationApiKeys.WEBPAY,
                    IntegrationType.TEST);
            this.tx = new WebpayPlus.Transaction(options);
        }
    }

    public WebpayPlusTransactionCreateResponse iniciarPago(String buyOrder, String sessionId, double amount) throws Exception {
        return tx.create(buyOrder, sessionId, amount, returnUrl);
    }

    public WebpayPlusTransactionCommitResponse confirmarPago(String tokenWs) throws Exception {
        return tx.commit(tokenWs);
    }
}

package com.company.kanban.dto;

import java.util.List;

public record ClientAccessResponse(
        String localUrl,
        String configuredBaseUrl,
        boolean companyAddressConfigured,
        boolean companyAddressUsable,
        String activationLinkBaseUrl,
        String suggestedNetworkUrl,
        List<String> detectedNetworkUrls,
        String guidance
) {}

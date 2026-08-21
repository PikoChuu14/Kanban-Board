package com.company.kanban.controller;

import com.company.kanban.dto.ClientAccessResponse;
import com.company.kanban.config.CompanyAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {
    private final int port;
    private final CompanyAddress companyAddress;

    public AdminSystemController(
            @Value("${server.port:8080}") int port,
            CompanyAddress companyAddress) {
        this.port = port;
        this.companyAddress = companyAddress;
    }

    @GetMapping("/client-access")
    public ClientAccessResponse clientAccess() {
        List<String> addresses = findLanAddresses();
        List<String> detectedUrls = addresses.stream()
                .map(address -> "http://" + address + ":" + port)
                .toList();
        String suggestedUrl = detectedUrls.size() == 1 ? detectedUrls.get(0) : null;
        String guidance = companyAddress.isUsable()
                ? "The configured company address is authoritative. Detected addresses are shown only for network troubleshooting."
                : companyAddress.validationMessage();
        return new ClientAccessResponse(
                "http://localhost:" + port,
                companyAddress.configuredUrl(),
                companyAddress.isConfigured(),
                companyAddress.isUsable(),
                companyAddress.activationBaseUrl(),
                suggestedUrl,
                detectedUrls,
                guidance);
    }

    private List<String> findLanAddresses() {
        List<String> result = new ArrayList<>();
        try {
            var interfaces = NetworkInterface.networkInterfaces()
                    .filter(this::isSuitableInterface)
                    .sorted(Comparator.comparing(NetworkInterface::getIndex))
                    .toList();
            for (NetworkInterface networkInterface : interfaces) {
                networkInterface.inetAddresses()
                        .filter(address -> address instanceof Inet4Address)
                        .filter(address -> !address.isLoopbackAddress() && !address.isLinkLocalAddress())
                        .map(address -> address.getHostAddress())
                        .filter(this::isPrivateIpv4)
                        .forEach(result::add);
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result.stream().distinct().toList();
    }

    private boolean isSuitableInterface(NetworkInterface networkInterface) {
        try {
            String identity = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase(Locale.ROOT);
            return networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual()
                    && !identity.matches(".*(vpn|virtual|vmware|hyper-v|vbox|docker|wsl|tunnel|tap|loopback|vethernet|wireguard|tailscale|zerotier|hamachi).*" );
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isPrivateIpv4(String address) {
        String[] parts = address.split("\\.");
        if (parts.length != 4) return false;
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 10 || (first == 172 && second >= 16 && second <= 31) || (first == 192 && second == 168);
    }

}

package com.company.kanban.controller;

import com.company.kanban.dto.ClientAccessResponse;
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
    private final String configuredBaseUrl;

    public AdminSystemController(
            @Value("${server.port:8080}") int port,
            @Value("${app.base-url:}") String configuredBaseUrl) {
        this.port = port;
        this.configuredBaseUrl = normalizeConfiguredUrl(configuredBaseUrl);
    }

    @GetMapping("/client-access")
    public ClientAccessResponse clientAccess() {
        List<String> addresses = findLanAddresses();
        List<String> detectedUrls = addresses.stream()
                .map(address -> "http://" + address + ":" + port)
                .toList();
        String suggestedUrl = detectedUrls.size() == 1 ? detectedUrls.get(0) : null;
        String guidance = configuredBaseUrl != null
                ? "The configured company address is authoritative. Detected addresses are shown only for network troubleshooting."
                : addresses.isEmpty()
                    ? "No suitable LAN IPv4 address was detected. Configure APP_BASE_URL to the stable hostname or address employees should use."
                    : addresses.size() > 1
                        ? "More than one suitable LAN address was detected. Do not choose an adapter silently; configure APP_BASE_URL to the stable company address."
                        : "This detected address is a temporary suggestion. Configure APP_BASE_URL before company rollout.";
        return new ClientAccessResponse("http://localhost:" + port, configuredBaseUrl, suggestedUrl, detectedUrls, guidance);
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

    private String normalizeConfiguredUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("/+$", "");
    }
}

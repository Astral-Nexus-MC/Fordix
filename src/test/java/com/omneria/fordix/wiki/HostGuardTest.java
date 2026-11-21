package com.omneria.fordix.wiki;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostGuardTest {
    private static boolean pub(String literal) throws Exception {
        return HostGuard.isPublic(InetAddress.getByName(literal));
    }

    @Test
    void theLocalNetworkIsRefused() throws Exception {
        for (String address : new String[]{"127.0.0.1", "10.1.2.3", "192.168.0.10", "172.16.5.5", "169.254.169.254", "0.0.0.0",
                "100.64.0.1", "198.18.0.1", "255.255.255.255", "::1", "fe80::1", "fc00::1", "fd12:3456::1"}) {
            assertFalse(pub(address), address);
        }
    }

    @Test
    void thePublicInternetIsAllowed() throws Exception {
        for (String address : new String[]{"8.8.8.8", "93.184.216.34", "1.1.1.1", "2606:4700:4700::1111"}) {
            assertTrue(pub(address), address);
        }
    }

    @Test
    void onlyWebAddressesPass() {
        assertThrows(IOException.class, () -> HostGuard.check(URI.create("file:///etc/passwd")));
        assertThrows(IOException.class, () -> HostGuard.check(URI.create("ftp://example.com/x")));
        assertThrows(IOException.class, () -> HostGuard.check(URI.create("http://127.0.0.1:8080/api")));
        assertThrows(IOException.class, () -> HostGuard.check(URI.create("http://[::1]/")));
        assertThrows(IOException.class, () -> HostGuard.check(URI.create("http://169.254.169.254/latest/meta-data")));
    }
}

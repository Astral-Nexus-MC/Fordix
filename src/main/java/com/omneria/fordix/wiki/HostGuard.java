package com.omneria.fordix.wiki;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * What the wiki importers may download from: web addresses whose host is on the public internet. An administrator types the address
 * and a page can redirect somewhere else, so the server must not be made to read its own machine or its local network (loopback, the
 * private ranges, link-local addresses such as cloud metadata services).
 */
final class HostGuard {
    private HostGuard() {
    }

    /** Whether an address is one of the public internet. */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // 100.64.0.0/10 (carrier-grade NAT), 192.0.0.0/24, 198.18.0.0/15 (benchmarks), 0.0.0.0/8 and the reserved 240.0.0.0/4.
            return !(first == 0 || first >= 240 || (first == 100 && second >= 64 && second <= 127)
                    || (first == 192 && second == 0 && (bytes[2] & 0xFF) == 0) || (first == 198 && (second == 18 || second == 19)));
        }
        // Unique local addresses (fc00::/7).
        return (bytes[0] & 0xFE) != 0xFC;
    }

    /** Throws when the address is not a web address of a public host. */
    static void check(URI uri) throws IOException {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("not a web address: " + uri);
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!isPublic(address)) {
                    throw new IOException("refused: " + host + " is not a public host");
                }
            }
        } catch (UnknownHostException e) {
            throw new IOException("unknown host " + host);
        }
    }
}

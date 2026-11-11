package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.data.auth.AuthLinkRequest;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthLinkService {
    private final OneTimeTokenService tokens;
    private final MailSender mailSender;
    private final AuthProperties properties;
    private final Map<String, Instant> emailRequests = new HashMap<>();
    private final Map<String, IpRequests> ipRequests = new HashMap<>();

    public void send(AuthLinkRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        checkRateLimit(email, clientIp);
        var token = tokens.generate(new GenerateOneTimeTokenRequest(email, properties.tokenValidity()));
        String link = UriComponentsBuilder.fromUri(properties.publicBaseUrl()).replacePath("/login/confirm")
                .queryParam("token", token.getTokenValue()).queryParam("redirect", safeRedirect(request.redirect()))
                .build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject("Sign in to PEEL WEB");
        message.setText("Confirm your sign-in to PEEL WEB:\n\n" + link
                + "\n\nThis link expires in " + properties.tokenValidity().toMinutes()
                + " minutes and can only be used once.\nIf you did not request it, ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // A failed delivery must not leave a usable token behind.
            tokens.consume(new OneTimeTokenAuthenticationToken(token.getTokenValue()));
            throw new AuthRequestException(503, "mail_unavailable", "The sign-in email could not be sent. Please try again later.");
        }
    }

    public static String normalizeEmail(String value) {
        String email = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        try {
            if (email.length() > 254 || !email.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")) throw new AddressException();
            new InternetAddress(email, true).validate();
            return email;
        } catch (AddressException exception) {
            throw new AuthRequestException(400, "invalid_email", "Enter a valid email address.");
        }
    }

    public static String safeRedirect(String path) {
        if (path == null || path.length() > 2048 || path.contains("\\") || path.contains("%")
                || path.chars().anyMatch(Character::isISOControl)) return "/";
        return path.matches("/(?:projects/[^/?#]+(?:/settings)?|invitations/[A-Za-z0-9_-]{43}|print-jobs(?:/[^/?#]+)?)(?:[?#].*)?") ? path : "/";
    }

    private synchronized void checkRateLimit(String email, String clientIp) {
        Instant now = Instant.now();
        emailRequests.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        ipRequests.entrySet().removeIf(entry -> !entry.getValue().resetAt().isAfter(now));
        Instant nextEmail = emailRequests.get(email);
        IpRequests ip = ipRequests.get(clientIp);
        if (nextEmail != null) throw limited(now, nextEmail);
        if (ip != null && ip.count() >= properties.requestsPerIpPerHour()) throw limited(now, ip.resetAt());
        emailRequests.put(email, now.plus(properties.resendInterval()));
        ipRequests.put(clientIp, ip == null ? new IpRequests(1, now.plusSeconds(3600))
                : new IpRequests(ip.count() + 1, ip.resetAt()));
    }
    private AuthRequestException limited(Instant now, Instant retryAt) {
        return new AuthRequestException(429, "rate_limited", "Please wait before requesting another sign-in email.",
                Math.max(1, retryAt.getEpochSecond() - now.getEpochSecond() + 1));
    }
    private record IpRequests(int count, Instant resetAt) {}
}
